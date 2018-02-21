package smartcardio;

import static com.izforge.izpack.util.Debug.log;
import java.util.List;
import java.util.Scanner;

import javax.smartcardio.ATR;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;
//Codigo ejemplo para trabajar con tarjetas inteligentes no funciona bien link:https://www.programcreek.com/java-api-examples/?api=javax.smartcardio.TerminalFactory
public class SmartCardIO {
    
    private byte[] atr = null;
    private String protocol = null;
    private byte[] historical = null;

    private static final byte[] SELECT_PKI_APPLET_CMD = { 0x00, (byte) 0xA4,
            0x04, 0x00, 0x06, (byte) 0xA0, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01 };

    private static final short SW_SUCCESS = (short) 0x9000;

    private final static byte PKI_APPLET_CLA = (byte) 0x80;
    private final static byte INS_VERIFY_PIN = (byte) 0x01;
    private final static byte INS_SIGN = (byte) 0x02;
    
    public CardTerminal selectCardTerminal() {
        try {
            // show the list of available terminals
            CardTerminal Reader = null;
            TerminalFactory factory = TerminalFactory.getDefault();
            CardTerminals terminals = factory.terminals();
            List<CardTerminal> terminal = terminals.list();
            System.out.println("Existen "+terminal.size()+" Lectores");
            for(int i=0;i<terminal.size();i++){
                Reader=terminal.get(i);
                if(Reader.isCardPresent()){
                    System.out.println((i+1)+". El lector: "+Reader+" tiene conectada una tarjeta");
                    return Reader;
                }
            }
            System.err.println("Ningun lector tiene conectada una tarjeta");
            return null;
        } catch (Exception e) {
            System.err.println("No hay lectores conectados");
            return null;
        }
    }

    public String byteArrayToHexString(byte[] b) {
        StringBuffer sb = new StringBuffer(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            int v = b[i] & 0xff;
            if (v < 16) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(v));
        }
        return sb.toString().toUpperCase();
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character
                    .digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public Card establishConnection(CardTerminal ct) {
        this.atr = null;
        this.historical = null;
        this.protocol = null;
        //System.out.println("Selecionar uno de estos protocolos (1-3):");
        //System.out.println("[1] - T=0");
        //System.out.println("[2] - T=1");
        //System.out.println("[3] - * [default]");
        String p = "*";
        /*Scanner in = new Scanner(System.in);
        try {
            int opt = in.nextInt();
            switch (opt){
                case 1:
                    p = "T=0";
                    break;
                case 2:
                    p = "T=1";
                    break;
                case 3:
                    p = "*";
                    break;
                default:
                    p = "*";
                    break;
            }
        } catch (Exception e) {
            System.err.println("Valor erroneo, ¡seleccionando el protocolo predeterminado!");
            p = "*";
        }*/
        Card card = null;
        try {
            //System.out.println("seleccionado: " + p);
            System.out.println("Conectando...");
            card = ct.connect(p);
        } catch (CardException e) {
            System.err.println("ERROR Conexion");
            return null;
        }
        ATR at = card.getATR();
        System.out.println("Conectado:");
        System.out.println(" - ATR:  " + byteArrayToHexString(at.getBytes()));
        System.out.println(" - Historial: "
                + byteArrayToHexString(at.getHistoricalBytes()));
        System.out.println(" - Protocolo: " + card.getProtocol());
        this.atr = at.getBytes();
        this.historical = at.getHistoricalBytes();
        this.protocol = card.getProtocol();
        return card;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        SmartCardIO pcsc = new SmartCardIO();
        char x='N';
        while(x!='S' && x!='s'){
            CardTerminal ct = pcsc.selectCardTerminal();
            Card c = null;
            if (ct != null) {
                c = pcsc.establishConnection(ct); 
                CardChannel cc = c.getBasicChannel();
                try {
                     ResponseAPDU answer = cc.transmit(new CommandAPDU(0xFF, 0xCA, 0x00, 0x00, 0x00));
                     System.out.println("Respuesta:\n" + answer.toString());
                     CommandAPDU cmd = new CommandAPDU(SELECT_PKI_APPLET_CMD);
                     ResponseAPDU response = transmit(cc, cmd);
                     checkSW(response);
                     
                } catch (CardException e) {
                    System.err.println(e);
                }
            }
            System.out.println("\nSALIR [S/N]");
            Scanner in = new Scanner(System.in);
            x=in.next().charAt(0);
        }
    }
    
    private static void checkSW(ResponseAPDU response) {
        if (response.getSW() != (SW_SUCCESS & 0xffff)) {
            System.err.printf("Received error status: %02X. Exiting.\n",response.getSW());
        }
    }
    
    private static ResponseAPDU transmit(CardChannel channel, CommandAPDU cmd)
            throws CardException {
        log(cmd);
        ResponseAPDU response = channel.transmit(cmd);
        log(response);

        return response;
    }
}
