#!/usr/bin/env python3
"""
RAFT (Retrieval Augmented Fine-Tuning) 데이터 생성 스크립트

이 스크립트는 SkillWeaver의 시드 데이터(JSON)를 기반으로
OpenAI Fine-tuning에 사용할 학습 데이터를 생성합니다.

Usage:
    python raft_data_generator.py --input-dir ../src/main/resources/knowledge-seed --output training_data.jsonl
"""

import os
import json
import random
import argparse
from pathlib import Path
from openai import OpenAI

client = OpenAI()

SYSTEM_PROMPT = """You are a helpful tech learning assistant. 
You MUST answer based ONLY on the provided context documents.
If the answer is not in the context, say "I don't have information about this in the provided context."
When answering, explain your reasoning step by step (Chain-of-Thought)."""

def load_seed_files(input_dir: str) -> list[dict]:
    """Load all JSON seed files from the directory."""
    seed_files = []
    for filepath in Path(input_dir).glob("*.json"):
        with open(filepath, 'r', encoding='utf-8') as f:
            data = json.load(f)
            seed_files.append({
                "filename": filepath.name,
                "technology": data.get("technology", "unknown"),
                "displayName": data.get("displayName", "Unknown"),
                "documents": data.get("documents", [])
            })
    return seed_files


def generate_qa_from_document(doc_content: str, tech_name: str) -> dict:
    """Use GPT-4o to generate a question-answer pair from a document."""
    response = client.chat.completions.create(
        model="gpt-4o",
        messages=[
            {"role": "system", "content": "You are a helpful assistant that generates training data for a tech learning AI."},
            {"role": "user", "content": f"""Based on the following document about {tech_name}, generate:
1. A realistic question that a developer might ask
2. A detailed Chain-of-Thought answer that explains reasoning step by step

Document:
{doc_content}

Respond in JSON format:
{{"question": "...", "answer": "Let me think through this step by step..."}}
"""}
        ],
        response_format={"type": "json_object"},
        temperature=0.7
    )
    
    return json.loads(response.choices[0].message.content)


def create_training_example(question: str, relevant_doc: str, distractor_docs: list[str], answer: str) -> dict:
    """Create a single training example in OpenAI fine-tuning format."""
    # Build context with relevant and distractor documents
    context_parts = [f"[RELEVANT]\n{relevant_doc}"]
    for i, distractor in enumerate(distractor_docs):
        context_parts.append(f"[DISTRACTOR-{i+1}]\n{distractor}")
    
    # Shuffle to make it harder
    random.shuffle(context_parts)
    context = "\n\n---\n\n".join(context_parts)
    
    return {
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": f"Question: {question}\n\n<context>\n{context}\n</context>"},
            {"role": "assistant", "content": answer}
        ]
    }


def main():
    parser = argparse.ArgumentParser(description="Generate RAFT training data")
    parser.add_argument("--input-dir", required=True, help="Directory containing seed JSON files")
    parser.add_argument("--output", default="training_data.jsonl", help="Output JSONL file path")
    parser.add_argument("--num-examples", type=int, default=50, help="Number of training examples to generate")
    parser.add_argument("--distractors", type=int, default=2, help="Number of distractor documents per example")
    args = parser.parse_args()
    
    print(f"Loading seed files from {args.input_dir}...")
    seed_files = load_seed_files(args.input_dir)
    print(f"Found {len(seed_files)} seed files")
    
    # Collect all documents
    all_docs = []
    for seed in seed_files:
        for doc in seed["documents"]:
            all_docs.append({
                "tech": seed["displayName"],
                "content": doc.get("content", ""),
                "type": doc.get("type", "UNKNOWN")
            })
    
    print(f"Total documents: {len(all_docs)}")
    
    training_data = []
    examples_per_file = max(1, args.num_examples // len(seed_files))
    
    for seed in seed_files:
        print(f"Processing {seed['displayName']}...")
        
        for doc in seed["documents"][:examples_per_file]:
            try:
                # Generate QA pair
                qa = generate_qa_from_document(doc["content"], seed["displayName"])
                
                # Select distractor documents from other technologies
                distractors = [
                    d["content"] for d in random.sample(
                        [d for d in all_docs if d["tech"] != seed["displayName"]],
                        min(args.distractors, len(all_docs) - 1)
                    )
                ]
                
                # Create training example
                example = create_training_example(
                    question=qa["question"],
                    relevant_doc=doc["content"],
                    distractor_docs=distractors,
                    answer=qa["answer"]
                )
                
                training_data.append(example)
                print(f"  Generated example for {doc.get('type', 'UNKNOWN')}")
                
            except Exception as e:
                print(f"  Error generating example: {e}")
                continue
        
        if len(training_data) >= args.num_examples:
            break
    
    # Save to JSONL
    with open(args.output, 'w', encoding='utf-8') as f:
        for example in training_data:
            f.write(json.dumps(example, ensure_ascii=False) + '\n')
    
    print(f"\n✅ Generated {len(training_data)} training examples")
    print(f"📁 Saved to {args.output}")
    print(f"\nNext steps:")
    print(f"1. Review the generated data: head -5 {args.output}")
    print(f"2. Upload to OpenAI: openai api files.create -f {args.output} -p fine-tune")
    print(f"3. Create fine-tuning job: openai api fine_tuning.jobs.create -t <file_id> -m gpt-4o-mini-2024-07-18")


if __name__ == "__main__":
    main()
