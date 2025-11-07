from __future__ import annotations
import argparse, json, re
from pathlib import Path
from typing import Iterable, Mapping, List, Dict

DEFAULT_SKILLS = [
    "java","c++","python","javascript","sql","docker","c#","php","spring","machine_learning"
]

PATTERNS = {
    "java": [r"\bjava\b"],
    "c++": [r"\bc\+\+\b"],
    "python": [r"\bpython\b"],
    "javascript": [r"\bjavascript\b", r"\bjs\b"],
    "sql": [r"\bsql\b", r"\bmysql\b", r"\bpostgres(?:ql)?\b", r"\bsqlite\b", r"\bmssql\b"],
    "docker": [r"\bdocker\b", r"\bcontainer(?:s|ization)?\b"],
    "c#": [r"\bc#\b", r"\bcsharp\b", r"\bc-sharp\b"],
    "php": [r"\bphp\b"],
    "spring": [r"\bspring\b", r"\bspring\s*boot\b"],
    "machine_learning": [r"\bmachine\s*learning\b", r"\bml\b"]
}

TAG_RE = re.compile(r"</?highlighttext>", re.I)

def clean_text(s: str) -> str:
    return TAG_RE.sub("", s or "").lower()

def load_input(path: Path) -> List[Mapping[str, object]]:
    data = json.loads(path.read_text(encoding="utf-8"))
    if isinstance(data, Mapping):
        return [data]
    if isinstance(data, Iterable):
        return list(data)  # type: ignore[return-value]
    raise SystemExit("Ожидается объект или массив объектов вакансий")

def normalize_skill_token(s: str) -> str:
    t = s.strip().lower()
    rep = {"c sharp":"c#","c-sharp":"c#","csharp":"c#","js":"javascript","ml":"machine_learning"}
    return rep.get(t, t)

def detect_skill(canon: str, hay: str, listed: Iterable[str]) -> int:
    in_list = any(normalize_skill_token(x) == canon for x in listed)
    if in_list:
        return 1
    pats = PATTERNS.get(canon, [])
    return 1 if any(re.search(p, hay) for p in pats) else 0

def build_matrix(vacancies: List[Mapping[str, object]], skills_vocab: List[str]) -> Dict[str, object]:
    positions = []
    for v in vacancies:
        title = str(v.get("title","")).strip() or "Unknown Role"
        desc = clean_text(str(v.get("description","")))
        text = (title + " " + desc).lower()
        listed = []
        raw_list = v.get("skills", [])
        if isinstance(raw_list, Iterable) and not isinstance(raw_list, (str, bytes)):
            listed = [str(x) for x in raw_list]
        row = {s: detect_skill(s, text, listed) for s in skills_vocab}
        positions.append({"title": title, "skills": row})
    return {"skills": skills_vocab, "positions": positions}

def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Преобразование JSON вакансий в матрицу skills/positions")
    p.add_argument("--input", required=True, type=Path, help="Файл с объектом или массивом вакансий")
    p.add_argument("--output", required=True, type=Path, help="Куда сохранить Parameters.json")
    p.add_argument("--skills", type=Path, help="Необязательный файл с массивом навыков для словаря")
    return p.parse_args()

def main() -> None:
    args = parse_args()
    vocab = DEFAULT_SKILLS if not args.skills else list(json.loads(args.skills.read_text(encoding="utf-8")))
    data = load_input(args.input)
    result = build_matrix(data, vocab)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

if __name__ == "__main__":
    main()
