import requests
import json
import time
import os

BASE_URL = "https://api.enem.dev/v1"
OUTPUT_FILE = "../app/src/main/assets/enem_math.json"

def get_exams():
    print("Buscando lista de exames...")
    response = requests.get(f"{BASE_URL}/exams")
    response.raise_for_status()
    return response.json()

def get_questions_for_exam(year, discipline="matematica"):
    print(f"Buscando questões de {discipline} para o ano {year}...")
    url = f"{BASE_URL}/exams/{year}/questions?discipline={discipline}"
    response = requests.get(url)
    if response.status_code == 404:
        print(f"  Sem questões para {year}")
        return []
    response.raise_for_status()
    return response.json()

def main():
    os.makedirs(os.path.dirname(OUTPUT_FILE), exist_ok=True)
    all_questions = []
    
    exams_data = get_exams()
    exams = exams_data if isinstance(exams_data, list) else exams_data.get("exams", exams_data)
    
    # If the root is a list, use it. If it's a dict, it might be the response wrapper
    if isinstance(exams, dict) and "exams" not in exams:
        # Based on previous log, it's just a list of objects or an object with a list.
        # Let's inspect the keys if it's a dict
        for k in exams.keys():
            if isinstance(exams[k], list):
                exams = exams[k]
                break

    for exam in exams:
        year = exam.get("year")
        if not year:
            continue
            
        questions = get_questions_for_exam(year)
        # Handle pagination if necessary, though the API might return all by default or wrap in a "questions" key
        if isinstance(questions, dict):
            # Sometimes APIs return { "questions": [...] }
            questions = questions.get("questions", questions.get("data", []))
            
        all_questions.extend(questions)
        time.sleep(1) # Polite delay
        
    print(f"Total de questões obtidas: {len(all_questions)}")
    
    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(all_questions, f, ensure_ascii=False, indent=2)
        
    print(f"Salvo em {OUTPUT_FILE}")

if __name__ == "__main__":
    main()
