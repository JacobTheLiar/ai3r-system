Przeanalizuj podany tekst i wyciągnij z niego:
1. Nazwy plików (szczególnie obrazów)
2. Dostępne operacje na plikach

Zwróć wynik w formacie JSON:
{
"operations": ["operacja1","operacja2"],
"photos": ["plik1.png", "plik2.png"]
}

Zasady:
- W "photos" umieść tylko nazwy plików (bez ścieżek/URL-i)
- W "operations" wyciągnij nazwy poleceń/operacji (często w nawiasach lub po słowach "polecenia:")
- Ignoruj tekst opisowy i ścieżki bazowe
- Jeśli plik ma rozszerzenie, zachowaj je