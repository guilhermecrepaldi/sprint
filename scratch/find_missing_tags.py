import re

with open('app/src/main/java/com/strava_matematica/model/MathCurriculum.kt', 'r', encoding='utf-8') as f:
    curriculum = f.read()

with open('app/src/main/java/com/strava_matematica/domain/procedural/ProceduralEngine.kt', 'r', encoding='utf-8') as f:
    engine = f.read()

tags_curr = set(re.findall(r'id\s*=\s*"([a-z_]+)"', curriculum))
tags_eng = set(re.findall(r'"([a-z_]+)"', engine))

missing = sorted(list(tags_curr - tags_eng))
print('MISSING TAGS:')
for t in missing:
    print(t)
