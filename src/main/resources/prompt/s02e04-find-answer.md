Przeanalizuj podane pliki i zwróć JSON z listą nazw plików podzieloną na kategorie. Klasyfikuj pliki zawierające informacje o złapanych/przechwyconych osobach do kategorii "people", a pliki dotyczące fizycznego sprzętu do kategorii "hardware".

Format odpowiedzi (tylko JSON, bez dodatkowych komentarzy):
{
  "people": ["nazwa_pliku1", "nazwa_pliku2"],
  "hardware": ["nazwa_pliku3", "nazwa_pliku4"]
}

Kryteria klasyfikacji:
- "people": pliki zawierające informacje o osobach, zatrzymaniach, aresztowaniach, przechwyceniach ludzi
- "hardware": pliki zawierające informacje o fizycznym sprzęcie, urządzeniach, technologii, narzędziach

WYKLUCZENIA z kategorii "hardware":
- oprogramowanie, aplikacje, programy
- aktualizacje, instalacje, wgrywanie programów
- systemy operacyjne, firmware
- bazy danych, pliki konfiguracyjne

Uwzględniaj tylko fizyczny sprzęt (komputery, telefony, kamery, itp.).