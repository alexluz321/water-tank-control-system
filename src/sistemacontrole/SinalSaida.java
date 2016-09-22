/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sistemacontrole;

import java.util.Random;
import javax.swing.ImageIcon;

/**
 *
 * @author alexandre
 */
public class SinalSaida {
    private static PID pid;
    
    private double sinal_calculado, sinal_tratado;
    ImageIcon diagramaON = new ImageIcon(LeituraEscritaCanais.class.getResource("/imagens/diagrama_on.png"));
    ImageIcon diagramaREV = new ImageIcon(LeituraEscritaCanais.class.getResource("/imagens/diagrama_reverse.png"));
    ImageIcon diagramaOFF = new ImageIcon(LeituraEscritaCanais.class.getResource("/imagens/diagrama_off.png"));

    private double amplitude, 
                    periodo, 
                    offset, 
                    periodoMinimo, 
                    periodoMaximo,
                    amplitudeMinima,
                    amplitudeMaxima,
                    duracao,
                    aleatorioInicial;
    
    private static boolean runSinal;
    
    public boolean isGerarNovoAleatorio, isMalhaFechada;
    
    private final LeituraEscritaCanais leituraEscritaCanais;
    private final FuncoesWindow funcoesWindow;
    
    Thread sinalSaida;
    
    SinalSaida(LeituraEscritaCanais leituraEscritaCanais, FuncoesWindow funcoesWindow, PID pid){
        this.leituraEscritaCanais = leituraEscritaCanais;
        this.funcoesWindow = funcoesWindow;
        SinalSaida.pid = pid;
        
        this.isGerarNovoAleatorio = true;
        runSinal = true;
        this.leituraEscritaCanais.criarGraficoSaida();
        
        int tipoOnda = 0;
        
        //se for degrau
        if("degrau".equals(this.funcoesWindow.GetFuncaoSelecionada())){
            this.amplitude = this.funcoesWindow.GetAmplitude();
            this.offset = this.funcoesWindow.GetOffset();
            tipoOnda = 2;
        }
        //se for senoide, serra ou quadrada
        else if(!"aleatoria".equals(this.funcoesWindow.GetFuncaoSelecionada())){
            this.amplitude = this.funcoesWindow.GetAmplitude();
            this.offset = this.funcoesWindow.GetOffset();
            this.periodo = this.funcoesWindow.GetPeriodo();

            if("senoidal".equals(this.funcoesWindow.GetFuncaoSelecionada())){
                tipoOnda = 1;   
            }
            else if("serra".equals(this.funcoesWindow.GetFuncaoSelecionada())){
                tipoOnda = 4;
            }
            else if("quadrada".equals(this.funcoesWindow.GetFuncaoSelecionada())){
                tipoOnda = 3;   
            }

        }
        else{//caso seja aleatorio
            this.offset = this.funcoesWindow.GetOffset();
            this.amplitudeMaxima = this.funcoesWindow.GetAmplitudeMaxima();
            this.amplitudeMinima = this.funcoesWindow.GetAmplitudeMinima();
            this.periodoMaximo = this.funcoesWindow.GetPeriodoMaximo();
            this.periodoMinimo = this.funcoesWindow.GetPeridoMinimo();
            tipoOnda = 5;
        }
        
        if(this.funcoesWindow.isMalhaFechada()){
            sinalSaida = new Thread(new setSaidaMalhaFechada(tipoOnda));
        }
        else{
            sinalSaida = new Thread(new setSaidaMalhaAberta(tipoOnda));
        }
        
        sinalSaida.start();
    }
    
    public class setSaidaMalhaFechada extends Thread{
        private int tipoOnda;
        setSaidaMalhaFechada(int tipoOnda){
            this.tipoOnda = tipoOnda;
        }
        
        @Override
        public void run(){
            while(runSinal){
                try{
                    switch(this.tipoOnda){
                    case 1://senoidal
                        gerarSenoidal();
                        break;
                    case 2://degrau
                        gerarDegrau();
                        leituraEscritaCanais.addSetPointCurva(sinal_calculado);
                        break;
                    case 3://quadrada
                        gerarQuadrada();
                        leituraEscritaCanais.addSetPointCurva(sinal_calculado);
                        break;
                    case 4://serra
                        gerarSerra();
                        break;
                    case 5://aleatoria
                        gerarAleatoria();
                        leituraEscritaCanais.addSetPointCurva(sinal_calculado);
                        break;
                    default:
                        System.out.println("Nenhuma onda selecionada.");
                        break;
                    }
                    SinalSaida.pid.setSetPoint(getSinalCalculado(), true);
                    setSinalCalculado(SinalSaida.pid.getValorCalculado());
                    checarTravas();
                    enviarBomba();
                    Thread.sleep(100);
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        }
    }
    
    class setSaidaMalhaAberta extends Thread{
        private int tipoOnda;
        setSaidaMalhaAberta(int tipoOnda){
            this.tipoOnda = tipoOnda;
        }
        
        public void run(){
            while(runSinal){
                switch(this.tipoOnda){
                    case 1://senoidal
                        gerarSenoidal();
                        break;
                    case 2://degrau
                        gerarDegrau();
                        break;
                    case 3://quadrada
                        gerarQuadrada();
                        break;
                    case 4://serra
                        gerarSerra();
                        break;
                    case 5://aleatoria
                        gerarAleatoria();
                        break;
                    default:
                        System.out.println("Nenhuma onda selecionada.");
                        break;
                }

                checarTravas();
                enviarBomba();

                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    System.out.println("Sleep parado.");
                }
            }
        }
    }
    
    class SemSinal extends Thread{
        @Override
        public void run(){
            while(runSinal){
                try{
                    setSinalCalculado(0);
                    SinalSaida.pid.setSetPoint(0, false);
                    checarTravas();
                    enviarBomba();
                    Thread.sleep(100);
                } catch (Exception e) {
                    System.out.println("SemSinal exceção :" + e);
                }
            }
        }
    }
    
    public void gerarSenoidal(){
        this.sinal_calculado = this.offset + (this.amplitude * Math.sin(Math.toRadians((getTempoGlobal() * 360) / this.periodo)));
    }
    
    public void gerarQuadrada(){
        this.sinal_calculado = this.amplitude;
            
        if(getTempoGlobal()%this.periodo > this.periodo/2){}
        else
        {
            this.sinal_calculado *= (-1);
        }
        
        this.sinal_calculado += this.offset;
    }
    
    public void gerarDegrau(){
        this.sinal_calculado = this.amplitude + this.offset;
    }
    
    public void gerarSerra(){
        this.sinal_calculado = this.offset + 2 * (this.amplitude/this.periodo) * (getTempoGlobal()%this.periodo) - this.amplitude;
    }
    
    public void gerarAleatoria(){
        if(this.isGerarNovoAleatorio){
            this.aleatorioInicial = this.leituraEscritaCanais.getTempoGlobal();
            Random r = new Random();
            this.duracao = (this.periodoMinimo) + ((this.periodoMaximo - this.periodoMinimo) * r.nextDouble());
            this.sinal_calculado = this.offset + (this.amplitudeMinima) + ((this.amplitudeMaxima - this.amplitudeMinima) * r.nextDouble());
            this.isGerarNovoAleatorio = false;
        }
        
        if(this.leituraEscritaCanais.getTempoGlobal() - this.aleatorioInicial >= this.duracao)
            this.isGerarNovoAleatorio = true;
    }
    
    public void checarTravas(){
        this.sinal_tratado = this.sinal_calculado;
        
        if (this.sinal_calculado > 4) {
            this.sinal_tratado = 4.0;
        }
        else if (this.sinal_calculado < -4) {
            this.sinal_tratado = -4.0;
        }
        
        if(this.leituraEscritaCanais.getCanalLeitura(0) > 28){
            if(this.sinal_calculado > 3.25){
                this.sinal_tratado = 2.9;
            }
            if(this.leituraEscritaCanais.getCanalLeitura(0) > 29){
                this.sinal_tratado = 0;
            }
        }
        else if(this.leituraEscritaCanais.getCanalLeitura(0) < 4 && this.sinal_calculado < 0){
            this.sinal_tratado = 0;
        }
        
        if(this.leituraEscritaCanais.getCanalLeitura(1) > 28){
            this.sinal_tratado = 0;
        }
    }

    //retorna o tempo atual do sistema
    public double getTempoGlobal(){
        return this.leituraEscritaCanais.getTempoGlobal();
    }
    
    //finaliza o sinal que estiver sendo enviado no momento
    public void stopSinal(boolean isNewSinal){
        runSinal = false;
        
        while(this.sinalSaida.isAlive()){
            try {
                Thread.sleep(5);
            } catch (InterruptedException ex) {
                System.out.println("stopSinal() Exception: "+ex);
            }
        }
        if(!isNewSinal){
            runSinal = true;
            this.sinalSaida = new Thread(new SemSinal());
            this.sinalSaida.start();
        }
    }
    
    //envia tensão para a bomba
    public void enviarBomba(){
        this.leituraEscritaCanais.setCanalSaida(0, this.sinal_tratado, this.sinal_calculado);

    }

    //get offset
    public double getOffset(){
        return this.offset;
    }
    
    public void setSinalCalculado(double num){
        this.sinal_calculado = num;
    }
    
    public double getSinalCalculado(){
        return this.sinal_calculado;
    }
    
    public double getSinalTratado(){
        return this.sinal_tratado;
    }
}
