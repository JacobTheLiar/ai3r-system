import 'dotenv/config';
import express from 'express';
import { InstructionService } from './instruction.service';

const app = express();
const PORT = process.env.PORT || 3000;
const instructionService = new InstructionService();

app.use(express.json());

interface RequestBody {
    instruction: string;
}

interface ResponseBody {
    description?: string;
    error?: string;
}

// @ts-ignore
app.post('/api/webhook', async (req: express.Request<{}, ResponseBody, RequestBody>, res: express.Response<ResponseBody>) => {
    const { instruction } = req.body;


    const baseResponsePayload: ResponseBody = {description: "", error: "ok"};

    if (!instruction || instruction.trim() === '') {
        return res.status(400).json({...baseResponsePayload, error: 'Pole "instruction" jest wymagane i nie może być puste.' });
    }

    try {
        const processedDescription = await instructionService.processInstruction(instruction);
        res.json({
            ...baseResponsePayload,
            description: processedDescription
        });
    } catch (error) {
        console.error('Error processing instruction:', error);
        let errorMessage = 'Wystąpił błąd podczas przetwarzania instrukcji.';
        if (error instanceof Error) {
            errorMessage = error.message;
        }
        res.status(500).json({...baseResponsePayload, error: errorMessage });
    }

});

app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
