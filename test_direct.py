import re
import urllib.request
import urllib.error

kt_file = r"D:\LOVE CLASS\app\src\main\java\com\strava_matematica\model\MathCurriculum.kt"

with open(kt_file, "r", encoding="utf-8") as f:
    content = f.read()

urls = re.findall(r'youtubeUrl\s*=\s*"([^"]+)"', content)
urls = list(set(urls)) # unique
print(f"Testando {len(urls)} URLs unicas...")

dead = []
for url in urls:
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
        with urllib.request.urlopen(req, timeout=5) as resp:
            html = resp.read().decode('utf-8', errors='ignore')
            # Extract title
            title_match = re.search(r'<title>(.*?)</title>', html)
            title = title_match.group(1) if title_match else ""
            
            # YouTube usually outputs "YouTube" or "Video unavailable - YouTube" or "404 Not Found"
            if title == "YouTube" or "Video unavailable" in title or "404 Not Found" in title:
                dead.append((url, "Suspicious Title: " + title))
            # Some playlists might be deleted, check if there's a specific sign
            
    except Exception as e:
        dead.append((url, str(e)))

if dead:
    print(f"Encontramos {len(dead)} links suspeitos ou mortos:")
    for link, reason in dead:
        print(f" - {link}: {reason}")
else:
    print("Todos os links parecem válidos e ativos!")
