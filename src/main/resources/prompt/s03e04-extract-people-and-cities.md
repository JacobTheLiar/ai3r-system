Przeanalizuj podany tekst i wyciągnij wszystkie wzmianki o osobach i miejscach. Zwróć wynik w formacie JSON:

Zasady:
- Osoby: TYLKO imię i jako oddzielne elementy
- Miasta: wszystkie nazwy miast, miejscowości, dzielnic
- Imiona w mianowniku (jeśli są w innych przypadkach, zamień na mianownik)
- Nazwy miast bez polskich znaków diakrytycznych (ą,ć,ę,ł,ń,ó,ś,ź,ż)
- Nazwy imion bez polskich znaków diakrytycznych (ą,ć,ę,ł,ń,ó,ś,ź,ż)
- Wszystko WIELKIMI LITERAMI
- Każde słowo osobno - np. "Barbara Zawadzka, Andrzej Zawadzki" to ["BARBARA", "ANDRZEJ"]
- Nie duplikuj - każde słowo tylko raz
- odpowiedź TYLKO w podanym formacie bez dodatkowych ozdobników np. markdown


Format odpowiedzi:
{
"peoples": ["BARBARA", "RAFAL"],
"cities": ["KRAKOW", "GDANSK"]
}