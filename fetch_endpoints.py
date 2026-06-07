import re
import os

engine_path = r"D:\LOVE CLASS\app\src\main\java\com\strava_matematica\domain\procedural\ProceduralEngine.kt"

with open(engine_path, "r", encoding="utf-8") as f:
    content = f.read()

# Match listOf("tag1", "tag2")
list_matches = re.findall(r'listOf\((.*?)\)', content)

# Match "tag1", "tag2" ->
when_matches = re.findall(r'((?:"[a-zA-Z0-9_]+"(?:,\s*)?)+)\s*->', content)

endpoints = set()

for match in list_matches:
    tags = re.findall(r'"([^"]+)"', match)
    for tag in tags:
        endpoints.add(tag)

for match in when_matches:
    tags = re.findall(r'"([^"]+)"', match)
    for tag in tags:
        endpoints.add(tag)

print("Endpoints Encontrados:")
for idx, tag in enumerate(sorted(endpoints), 1):
    print(f"{idx}. {tag}")
