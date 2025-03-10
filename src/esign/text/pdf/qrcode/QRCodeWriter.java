package esign.text.pdf.qrcode;

import java.util.Map;

public final class QRCodeWriter {

    private static final int QUIET_ZONE_SIZE = 4;

    public ByteMatrix encode(String contents, int width, int height) throws WriterException {
        return encode(contents, width, height, null);
    }

    public ByteMatrix encode(String contents, int width, int height, Map<EncodeHintType, Object> hints) throws WriterException {
        if (contents == null || contents.length() == 0) {
            throw new IllegalArgumentException("Found empty contents");
        }

        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Requested dimensions are too small: " + width + 'x' + height);
        }

        ErrorCorrectionLevel errorCorrectionLevel = ErrorCorrectionLevel.L;
        if (hints != null) {
            ErrorCorrectionLevel requestedECLevel = (ErrorCorrectionLevel) hints.get(EncodeHintType.ERROR_CORRECTION);
            if (requestedECLevel != null) {
                errorCorrectionLevel = requestedECLevel;
            }
        }

        QRCode code = new QRCode();
        Encoder.encode(contents, errorCorrectionLevel, hints, code);
        return renderResult(code, width, height);
    }

    private static ByteMatrix renderResult(QRCode code, int width, int height) {
        ByteMatrix input = code.getMatrix();
        int inputWidth = input.getWidth();
        int inputHeight = input.getHeight();
        int qrWidth = inputWidth + 8;
        int qrHeight = inputHeight + 8;
        int outputWidth = Math.max(width, qrWidth);
        int outputHeight = Math.max(height, qrHeight);

        int multiple = Math.min(outputWidth / qrWidth, outputHeight / qrHeight);

        int leftPadding = (outputWidth - inputWidth * multiple) / 2;
        int topPadding = (outputHeight - inputHeight * multiple) / 2;

        ByteMatrix output = new ByteMatrix(outputWidth, outputHeight);
        byte[][] outputArray = output.getArray();

        byte[] row = new byte[outputWidth];

        for (int y = 0; y < topPadding; y++) {
            setRowColor(outputArray[y], (byte) -1);
        }

        byte[][] inputArray = input.getArray();
        for (int i = 0; i < inputHeight; i++) {

            for (int x = 0; x < leftPadding; x++) {
                row[x] = -1;
            }

            int k = leftPadding;
            int m;
            for (m = 0; m < inputWidth; m++) {
                byte value = (inputArray[i][m] == 1) ? (byte)0 : (byte)-1;
                for (int n = 0; n < multiple; n++) {
                    row[k + n] = value;
                }
                k += multiple;
            }

            k = leftPadding + inputWidth * multiple;
            for (m = k; m < outputWidth; m++) {
                row[m] = -1;
            }

            k = topPadding + i * multiple;
            for (int z = 0; z < multiple; z++) {
                System.arraycopy(row, 0, outputArray[k + z], 0, outputWidth);
            }
        }

        int offset = topPadding + inputHeight * multiple;
        for (int j = offset; j < outputHeight; j++) {
            setRowColor(outputArray[j], (byte) -1);
        }

        return output;
    }

    private static void setRowColor(byte[] row, byte value) {
        for (int x = 0; x < row.length; x++) {
            row[x] = value;
        }
    }
}
