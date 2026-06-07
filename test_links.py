import re
import urllib.request
import urllib.error
import json
import time

kt_file = r"D:\LOVE CLASS\app\src\main\java\com\strava_matematica\model\MathCurriculum.kt"

with open(kt_file, "r", encoding="utf-8") as f:
    content = f.read()

# Match youtubeUrl = "https://..."
urls = re.findall(r'youtubeUrl\s*=\s*"([^"]+)"', content)

print(f"Encontrados {len(urls)} links do YouTube para testar...")

dead_links = []
good_links = 0

for i, url in enumerate(urls, 1):
    # Prepare oEmbed URL
    oembed_url = f"https://www.youtube.com/oembed?url={url}&format=json"
    try:
        req = urllib.request.Request(oembed_url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req) as response:
            if response.status == 200:
                good_links += 1
    except urllib.error.HTTPError as e:
        dead_links.append((url, e.code))
    except Exception as e:
        dead_links.append((url, str(e)))
        
    # Be nice to the API
    time.sleep(0.1)

print(f"\nTeste Concluído: {good_links} Links OK.")
if dead_links:
    print(f"Links com problema ({len(dead_links)}):")
    for link, err in dead_links:
        print(f" - {link} (Erro: {err})")
else:
    print("Nenhum link quebrado encontrado!")
