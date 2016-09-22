/*
Sistema de Controle LAB 1
Alexandre Luz, Jaime Dantas, Anderson e Higo Bessa
 */
package sistemacontrole;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.swing.JOptionPane;
import org.jfree.data.xy.XYSeries;


/**
 *
 * @author jaime
 */
public class SistemaControle {
    private static LoginWindow JanelaConectar;
    private static MainWindow JanelaPrincipal;
    private static FuncoesWindow JanelaFuncao;
    private static LogWindow logWindow;
    private static LeituraEscritaCanais leituraEscritaCanais;
    private static SinalSaida sinalSaida;
    private static PID pid;

    //cria conexao
    QuanserClient quanserClient;
    
    //variaveis
    double offset, periodo, amplitude, amplitudeMaxima, amplitudeMinima, periodoMaximo,
            periodoMinimo, tempo = 0;
    
    double[] tempoEntrada = new double[7], leitura = new double[7];
    
    double setPoint;
    
    //threads de saidas
    Thread saida0 = null;
    Thread saida1 = null;
    Thread[] entrada = new Thread[7];

    //variavel de malha fechada
    boolean malhaFechadaAtivada;
    
    //variaveis de parada das threads de saida
    boolean saida0isRunning = false, saida1isRunning = false;
    
    boolean[] entradaIsRunning = new boolean[7];
    
    static final XYSeries sinal_gerado = new XYSeries("Saida 0");
    static final XYSeries[] sinal_entrada = new XYSeries[7];

    //Construtor
    SistemaControle() throws IOException{
        //criar tela de login
        JanelaConectar = new LoginWindow();
        
        //criar tela de funcoes
        JanelaFuncao = new FuncoesWindow();
        
        //criar tela principal
        JanelaPrincipal = new MainWindow();
        
        //criar janela de logs
        logWindow = new LogWindow();
        
        JanelaConectar.addConectarListener(new ConnectListener());
        JanelaConectar.pack();
        JanelaConectar.setLocationRelativeTo(null);
        JanelaConectar.setTitle("Conectar");
        JanelaConectar.setVisible(true);
        
        JanelaPrincipal.addLerListener(new LerCanais());
        JanelaPrincipal.addTipoFuncaoListener(new EscolherTipoDeFuncao());
        JanelaPrincipal.addPararSinalListener(new PararSinal());
        JanelaPrincipal.setPreferredSize(new Dimension(900, 773));
        JanelaPrincipal.pack();
        JanelaPrincipal.setLocationRelativeTo(null);
        JanelaPrincipal.setTitle("Sistema de Controle de Planta Quanser");
        JanelaPrincipal.addLogWindowOpenListener(new ShowLogWindow());


        JanelaFuncao.addGerarFuncaoListener(new GerarFuncao());
        JanelaFuncao.pack();
        JanelaFuncao.setLocationRelativeTo(null);
        JanelaFuncao.setTitle("Escolha o tipo de função");
        JanelaFuncao.setDefaultCloseOperation(JanelaFuncao.DISPOSE_ON_CLOSE);

        logWindow.pack();
        logWindow.setLocationRelativeTo(null);
        logWindow.setTitle("Logs");
    }
    /**
     * @param args the command line arguments
     */
    
    
    public static void main(String[] args) throws IOException {
        //chama classe principal
        SistemaControle Sistema = new SistemaControle();
    }
    
    class EscolherTipoDeFuncao implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            JanelaFuncao.setVisible(true);
        }
    }
    
    class LerCanais implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            getLeituraEscrita().gerarGraficosEntrada();
        }
    }
    
    class ConnectListener implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e){
            //iniciar leitura dos canais
            leituraEscritaCanais = new LeituraEscritaCanais(JanelaConectar, JanelaPrincipal);
            
            JanelaConectar.setVisible(false);
            JanelaPrincipal.setVisible(true);
            leituraEscritaCanais.iniciarThreads();
            
            SistemaControle.pid = new PID(SistemaControle.JanelaFuncao, SistemaControle.leituraEscritaCanais, SistemaControle.JanelaPrincipal);
        }
    }
    
    class PararSinal implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent e) {
            try{
                sinalSaida.stopSinal(false);
            } catch (Exception ex){
                JOptionPane.showMessageDialog(null, "Nenhuma saída atualmente ativa.", "Aviso!", JOptionPane.WARNING_MESSAGE);
            }
        }
        
    }
    
    class ShowLogWindow implements ActionListener{
        
        @Override
        public void actionPerformed(ActionEvent e){
            JOptionPane.showMessageDialog(null, "Nenhuma saída atualmente ativa.", "Aviso!", JOptionPane.WARNING_MESSAGE);
            logWindow.setVisible(true);
        }
    }
    
    class GerarFuncao implements ActionListener{
        
        @Override
        public void actionPerformed(ActionEvent e) {
            try{
                sinalSaida.stopSinal(true);
                
            } catch (Exception ex){
//                System.out.println("Nenhuma thread rodando.");
            }
            if(SistemaControle.JanelaFuncao.isMalhaFechada()){
                JanelaFuncao.erro = "";
                int tipoControle = SistemaControle.JanelaFuncao.getSelectedControle();
                double paramPID[] = SistemaControle.JanelaFuncao.getPIDValores();
                boolean isKi = SistemaControle.JanelaFuncao.isKi();
                boolean isKd = SistemaControle.JanelaFuncao.isKd();
                
                SistemaControle.pid.setTipoControle(tipoControle);
                SistemaControle.pid.setPIDParametros(paramPID[0], paramPID[1], isKi, paramPID[2], isKd);
            }
            else{
                sinalSaida = new SinalSaida(SistemaControle.leituraEscritaCanais, SistemaControle.JanelaFuncao, SistemaControle.pid);
                JanelaFuncao.setVisible(false);

            }
            
            if(SistemaControle.JanelaFuncao.isMalhaFechada() && !SistemaControle.JanelaFuncao.theresError){
                JanelaFuncao.setVisible(false);
                sinalSaida = new SinalSaida(SistemaControle.leituraEscritaCanais, SistemaControle.JanelaFuncao, SistemaControle.pid);
            }
            else if(SistemaControle.JanelaFuncao.isMalhaFechada() && SistemaControle.JanelaFuncao.theresError){
                JanelaFuncao.mostrarErro();
            }
        }
    }
    
    public LeituraEscritaCanais getLeituraEscrita(){
        return SistemaControle.leituraEscritaCanais;
    }

}
