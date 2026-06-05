import csv
import re

csv_file = r'D:\LOVE CLASS\referencias\arvore_matematica_youtube.csv'
kt_file = r'D:\LOVE CLASS\app\src\main\java\com\strava_matematica\model\MathCurriculum.kt'

with open(csv_file, 'r', encoding='utf-8') as f:
    reader = csv.reader(f, delimiter=';')
    next(reader) # skip header
    csv_data = list(reader)

with open(kt_file, 'r', encoding='utf-8') as f:
    content = f.read()

for row in csv_data:
    if len(row) < 5: continue
    domain, sub, topic, channel, url = row
    
    # Let's escape topic for regex, but we only need substring matching.
    # We want to replace it with: name = "topic", youtubeChannel = "channel", youtubeUrl = "url"
    
    # Find the node that has exactly this name
    pattern = r'(name\s*=\s*\"' + re.escape(topic) + r'\")'
    
    if url in content:
        continue
    
    replacement = f'\\1, youtubeChannel = "{channel}", youtubeUrl = "{url}"'
    content = re.sub(pattern, replacement, content)

with open(kt_file, 'w', encoding='utf-8') as f:
    f.write(content)

print("Patch complete!")
