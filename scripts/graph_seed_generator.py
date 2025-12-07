#!/usr/bin/env python3
"""
Neo4j Graph Seed Generator

기존 knowledge-seed JSON 파일에서 기술 관계를 AI로 추출하여
Neo4j에 로딩할 수 있는 Cypher 스크립트와 JSON을 생성합니다.

Usage:
    python graph_seed_generator.py --input-dir ../src/main/resources/knowledge-seed --output graph_seed.cypher
"""

import os
import json
import argparse
from pathlib import Path
from openai import OpenAI

client = OpenAI()

# 기술 관계 타입 정의
RELATION_TYPES = [
    "DEPENDS_ON",       # 선행 지식 필요
    "EXTENDS",          # 확장/기반
    "CONTAINS",         # 포함 관계
    "RECOMMENDED_AFTER", # 학습 순서
    "ALTERNATIVE_TO",   # 대안
    "USED_WITH"         # 함께 사용
]

TECH_CATEGORIES = [
    "LANGUAGE", "FRAMEWORK", "LIBRARY", "TOOL", "CONCEPT", "DATABASE"
]

DIFFICULTY_LEVELS = ["BEGINNER", "INTERMEDIATE", "ADVANCED", "EXPERT"]


def load_seed_files(input_dir: str) -> list[dict]:
    """Load all JSON seed files from the directory."""
    seed_files = []
    for filepath in Path(input_dir).glob("*.json"):
        with open(filepath, 'r', encoding='utf-8') as f:
            data = json.load(f)
            seed_files.append({
                "filename": filepath.name,
                "technology": data.get("technology", filepath.stem),
                "displayName": data.get("displayName", filepath.stem),
                "documents": data.get("documents", [])
            })
    return seed_files


def extract_relationships(seed_files: list[dict]) -> dict:
    """Use GPT-4o to extract technology relationships from seed data."""
    
    # 기술 목록 생성
    tech_list = [f"{s['technology']} ({s['displayName']})" for s in seed_files]
    
    prompt = f"""You are a technology expert. Analyze the following list of technologies and create a knowledge graph of their relationships.

Technologies:
{chr(10).join(tech_list)}

For each technology, identify:
1. category: One of {TECH_CATEGORIES}
2. difficulty: One of {DIFFICULTY_LEVELS}
3. relations: List of relationships to other technologies in the list

Relationship types:
- DEPENDS_ON: Prerequisite knowledge (e.g., spring-boot DEPENDS_ON java)
- EXTENDS: Builds upon (e.g., spring-boot EXTENDS spring-framework)
- CONTAINS: Part of (e.g., spring-framework CONTAINS spring-core)
- RECOMMENDED_AFTER: Learning order (e.g., react RECOMMENDED_AFTER javascript)
- ALTERNATIVE_TO: Alternative choice (e.g., pytorch ALTERNATIVE_TO tensorflow)
- USED_WITH: Commonly used together (e.g., react USED_WITH tailwind)

IMPORTANT: Only create relationships between technologies in the provided list.

Respond in JSON format:
{{
  "nodes": [
    {{"name": "java", "displayName": "Java", "category": "LANGUAGE", "difficulty": "INTERMEDIATE"}},
    ...
  ],
  "edges": [
    {{"from": "spring-boot", "to": "java", "type": "DEPENDS_ON"}},
    {{"from": "spring-boot", "to": "spring-framework", "type": "EXTENDS"}},
    ...
  ]
}}
"""

    response = client.chat.completions.create(
        model="gpt-4o",
        messages=[
            {"role": "system", "content": "You are a technology relationship analyzer. Output valid JSON only."},
            {"role": "user", "content": prompt}
        ],
        response_format={"type": "json_object"},
        temperature=0.3
    )
    
    return json.loads(response.choices[0].message.content)


def generate_cypher(graph_data: dict) -> str:
    """Generate Cypher script for Neo4j."""
    lines = [
        "// Neo4j Graph Seed - Auto-generated",
        "// Run this script in Neo4j Browser or via cypher-shell",
        "",
        "// Clear existing data (optional)",
        "// MATCH (n) DETACH DELETE n;",
        "",
        "// Create Technology nodes"
    ]
    
    # Create nodes
    for node in graph_data.get("nodes", []):
        lines.append(f"""CREATE (:Technology {{
  name: "{node['name']}",
  displayName: "{node['displayName']}",
  category: "{node['category']}",
  difficulty: "{node['difficulty']}"
}});""")
    
    lines.append("")
    lines.append("// Create relationships")
    
    # Create edges
    for edge in graph_data.get("edges", []):
        lines.append(f"""MATCH (a:Technology {{name: "{edge['from']}"}}), (b:Technology {{name: "{edge['to']}"}})
CREATE (a)-[:{edge['type']}]->(b);""")
    
    return "\n".join(lines)


def generate_json_with_relations(seed_files: list[dict], graph_data: dict, output_dir: str):
    """Update JSON files with relationship information."""
    
    # Build edge lookup
    edge_lookup = {}
    for edge in graph_data.get("edges", []):
        from_tech = edge["from"]
        if from_tech not in edge_lookup:
            edge_lookup[from_tech] = []
        edge_lookup[from_tech].append({"to": edge["to"], "type": edge["type"]})
    
    # Build node lookup
    node_lookup = {n["name"]: n for n in graph_data.get("nodes", [])}
    
    # Update each seed file
    for seed in seed_files:
        tech_name = seed["technology"]
        node_info = node_lookup.get(tech_name, {})
        relations = edge_lookup.get(tech_name, [])
        
        updated_data = {
            "technology": tech_name,
            "displayName": seed["displayName"],
            "category": node_info.get("category", "CONCEPT"),
            "difficulty": node_info.get("difficulty", "INTERMEDIATE"),
            "relations": relations,
            "documents": seed["documents"]
        }
        
        output_path = Path(output_dir) / seed["filename"]
        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(updated_data, f, ensure_ascii=False, indent=2)
        
        print(f"  Updated: {seed['filename']} ({len(relations)} relations)")


def main():
    parser = argparse.ArgumentParser(description="Generate Neo4j graph seed from knowledge-seed JSON files")
    parser.add_argument("--input-dir", required=True, help="Directory containing seed JSON files")
    parser.add_argument("--output", default="graph_seed.cypher", help="Output Cypher script path")
    parser.add_argument("--update-json", action="store_true", help="Update JSON files with relations")
    args = parser.parse_args()
    
    print(f"Loading seed files from {args.input_dir}...")
    seed_files = load_seed_files(args.input_dir)
    print(f"Found {len(seed_files)} seed files")
    
    print("\nExtracting technology relationships using GPT-4o...")
    graph_data = extract_relationships(seed_files)
    print(f"Extracted {len(graph_data.get('nodes', []))} nodes and {len(graph_data.get('edges', []))} edges")
    
    # Generate Cypher script
    cypher_script = generate_cypher(graph_data)
    with open(args.output, 'w', encoding='utf-8') as f:
        f.write(cypher_script)
    print(f"\n✅ Cypher script saved to: {args.output}")
    
    # Save JSON graph data
    json_output = args.output.replace('.cypher', '.json')
    with open(json_output, 'w', encoding='utf-8') as f:
        json.dump(graph_data, f, ensure_ascii=False, indent=2)
    print(f"✅ JSON graph data saved to: {json_output}")
    
    # Optionally update original JSON files
    if args.update_json:
        print(f"\nUpdating JSON files with relations...")
        generate_json_with_relations(seed_files, graph_data, args.input_dir)
        print("✅ JSON files updated with relations")
    
    print(f"\n📊 Summary:")
    print(f"   Nodes: {len(graph_data.get('nodes', []))}")
    print(f"   Edges: {len(graph_data.get('edges', []))}")
    print(f"\nNext steps:")
    print(f"1. Review the generated Cypher: cat {args.output}")
    print(f"2. Run in Neo4j Browser or: cypher-shell -f {args.output}")
    print(f"3. Optionally run with --update-json to add relations to JSON files")


if __name__ == "__main__":
    main()
