import OpenAI from 'openai';

export class InstructionService {
    private openai: OpenAI;


    constructor() {
        this.openai = new OpenAI({
            apiKey: process.env.OPENAI_API_KEY,
        });
        console.log('InstructionService initialized');
    }

    async processInstruction(instruction: string): Promise<string> {
        console.log('process instruction:', instruction);

        try {
            const completion = await this.openai.chat.completions.create({
                model: "gpt-4o",
                messages: [
                    {
                        role: "system",
                        content: this.SYSTEM_PROMPT
                    },
                    {
                        role: "user",
                        content: instruction
                    }
                ]
            });

            const response = completion.choices[0]?.message?.content || "Brak odpowiedzi z API";
            console.log(' - response:', response);

            return response;
        } catch (error) {
            console.error('OpenAI API Error:', error);
            throw new Error('Błąd podczas komunikacji z OpenAI API');
        }
    }


    private readonly SYSTEM_PROMPT = `
Jesteś przewodnikiem po mapie 4x4. Pozycja startowa to górny lewy róg (1,1).

Mapa (współrzędne: kolumna, rząd):
(1,1): pin lokalizacji - START
(2,1): pole  
(3,1): drzewo
(4,1): dom

(1,2): pole
(2,2): młyn
(3,2): pole  
(4,2): pole

(1,3): pole
(2,3): pole
(3,3): skały
(4,3): drzewa

(1,4): góry
(2,4): góry
(3,4): samochód
(4,4): jaskinia

Ruchy: prawo = +1 kolumna, lewo = -1 kolumna, dół = +1 rząd, góra = -1 rząd

WAŻNE: Jeśli podano kilka ruchów, wykonuj je SEKWENCYJNIE:
- Zacznij od pozycji startowej (1,1)
- Wykonaj pierwszy ruch, oblicz nową pozycję
- Wykonaj drugi ruch z tej nowej pozycji
- Kontynuuj aż do końca sekwencji
- Podaj obiekt na końcowej pozycji

Przykład: "dwa pola w prawo i jedno w dół" = prawo, prawo, dół = (1,1)→(2,1)→(3,1)→(3,2)

Odpowiadaj tylko nazwą obiektu na końcowej pozycji. Jeśli którykolwiek ruch wykracza poza mapę, odpowiedz "poza mapą".`;
}
