/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sistemacontrole;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author alexandre
 */
public class PID {
    
    private final FuncoesWindow funcoesWindow;
    private final LeituraEscritaCanais leituraEscrita;

    
    //variáveis de controle
    private double kp, ki, kd, ti, td;
    
    //diz se foi setado kd ou td, e ki ou ti
    private boolean isKi, isKd;
    
    //variáveis de erro usadas no programa
    private double erro, erroSoma, erroDif, ultimoErro, erroDifD2;
    
    //ultimo valor registrado do sensor para ser usado no controlador PI-D
    private double ultimoValorSensor;
    
    //valores de cada parâmetro do alg de controle
    private double P, I, D, D2;
    
    //setPoint do controlador
    private double setPoint;
    private double setPointAnterior;
    
    //resultado final do sinal de controle
    private double sinal_calculado;
    
    //diz que tipo de controle foi selecionado
    private int tipoControle;
    
    //periodo de amostragem
    private double sampleTime;
    
    //anti windup variaveis
    private double saturacao;
//    private double antiWindUpValor;
            
    PID(FuncoesWindow funcoesWindow, LeituraEscritaCanais leituraEscrita){
        this.funcoesWindow = funcoesWindow;
        this.leituraEscrita = leituraEscrita;
        
        this.setPoint = 0;
        this.setPointAnterior = 0;
        
        this.erroSoma = 0;
        this.ultimoErro = 0;
        this.ultimoValorSensor = 0;
        
        this.sampleTime = 0.1;
        
        this.P = 0;
        this.I = 0;
        this.D = 0;
        this.D2 = 0;
    }
    
    public void calcularP(){
        this.P = this.kp * this.erro;        
    }
    
    public void calcularI(){
        if(this.isKi){
            //this.I = this.ki * this.erroSoma * this.sampleTime;
            /*
                para evitar sinais bruscos com mudança no valor de ki
                calcula-se o valor de I atraves de ki*erroAtual + valorAntigoDeI
            */
            this.I += this.ki * this.erro * this.sampleTime; 
        }
        else{
            if(this.ti == 0){
                this.I = 0;
            }
            else{
                //this.I = (this.kp/this.ti)*this.erroSoma * this.sampleTime;
                this.I += (this.kp/this.ti)*this.erro * this.sampleTime;
            }
        }
    }
    
    public void calcularD(){
        if(this.isKd){
            this.D = (this.kd * this.erroDif)/this.sampleTime;
        }
        else{
            this.D = (this.kp*this.td)*this.erroDif/this.sampleTime;
        }
    }
    
    public void calcularD2(){
        if(this.isKd){
            this.D = this.kd * this.erroDifD2/this.sampleTime;
        }
        else{
            this.D = (this.kp*this.td)*this.erroDifD2/this.sampleTime;
        }
    }
    
    public void calcularErros(){
        double leituraCanal = this.leituraEscrita.getCanalLeitura(this.funcoesWindow.getPV());
        this.erro = this.setPoint - leituraCanal;
        this.erroSoma += this.erro;
        this.erroDif = this.erro - this.ultimoErro;
        this.erroDifD2 = leituraCanal - this.ultimoValorSensor;
        
        this.ultimoValorSensor = leituraCanal;
        this.ultimoErro = this.erro;
    }
    
    public void calcularPID(){
        calcularP();
        calcularI();
        calcularD();
        calcularD2();
    }
    
    public double getValorCalculado(){
        calcularErros();
        calcularPID();
        
        //aplicar filtros
        //se o windup estiver selecionado
        if(this.funcoesWindow.isAntiWindUpActive()){
            runAntiWindUp();
        }
        
        //aplicar controle
        switch(this.tipoControle){
            case 0://controle tipo P
                this.sinal_calculado = this.P;
                break;
            case 1://controle tipo PI
                this.sinal_calculado = this.P + this.I;
                break;
            case 2://controle tipo PD
                this.sinal_calculado = this.P + this.D;
                break;
            case 3://controle tipo PID
                this.sinal_calculado = this.P + this.I + this.D;
                break;
            case 4://controle tipo PI-D
                this.sinal_calculado = this.P + this.I + this.D2;
                break;
            default:
                break;
        }
        return this.sinal_calculado;
    }

    //setar o tipo de controle utilizado
    public void setTipoControle(int tipoControle){
        this.tipoControle = tipoControle;
    }
    
    //setar os valores de kp, ki (ou ti) e kd (ou td)
    public void setPIDParametros(double kp, double i, boolean isKi, double d, boolean isKd){
        this.kp = kp;
        this.isKi = isKi;
        this.isKd = isKd;
        
        if(isKi){
            this.ki = i;
        }
        else{
            this.ti = i;
        }
        
        if(isKd){
            this.kd = d;
        }
        else{
            this.td = d;
        }
    }
    
    //aplica o setpoint do controle
    public void setSetPoint(double setPoint){
        this.setPointAnterior = this.setPoint;
        this.setPoint = setPoint;
        //calcular analise do sistema para novo valor de set point
        runAnaliseSistema();
    }
    
    //retorna o sample time do controle
    public void setSampleTime(double sampleTime){
        this.sampleTime = sampleTime;
    }
    
    //aplicar filtro wind up ao sinal de controle
    public void runAntiWindUp(){
//        this.saturacao = (this.sinal_calculado - travasWindUp(this.sinal_calculado)) / this.antiWindUpValor;
//        this.I = this.erroSoma + this.sampleTime*(this.ki * this.erro - this.kp * this.saturacao);
//        this.erroSoma += (this.ki * this.erro - this.kp * this.saturacao) * this.sampleTime;

        if(this.I > 4){
            this.I = 4;
        }
        else if(this.I < -4){
            this.I = -4;
        }
    }
    
    //retorna o valor do sensor atualmente selecionado
    public double getSensorValor(){
        return this.leituraEscrita.getCanalLeitura(this.funcoesWindow.getPV());
    }
    
    //calcular valores de sobressinal, tempo de subida(90%, 95% e 100%) 
    //tempo de pico, tempo de acomodação(2%, 5% e 10%)
    public void runAnaliseSistema(){
        Thread t = new Thread(new AnaliseSistema());
        t.start();
    }
    
    //classe que mede os tempos do sistema
    public class AnaliseSistema extends Thread{
        @Override
        public void run(){
            String _setPoint = setPointAnterior + "cm - " + setPoint + "cm";
            long startTime = System.currentTimeMillis();//inicio de medição
            long Tpico = 0, Ts2 = 0, Ts5 = 0, Ts10 = 0, Tr100 = 0, Tr95 = 0, Tr90 = 0,
                 startTr95 = 0, startTr90 = 0;
            double Mpcm = 0, Mp_porcentagem = 0;
            boolean endTpico=false, endTs2=false, endTs5=false, endTs10=false, 
                    endTr100=false, endTr95=false, endTr90=false, endMp=false,
                    startedTimeTr95=false, startedTimeTr90=false;
            double valorSensor;
            double set = (setPoint - setPointAnterior);
            
            while(true){
                try {
                    valorSensor = getSensorValor();
                    if(setPoint - setPointAnterior > 0){
                        if(valorSensor >= ((set)*0.1)+setPointAnterior && !startedTimeTr90){
                            startTr90 = System.currentTimeMillis();//inicio de contagem para 10-90%
                            startedTimeTr90 = true;
                        }
                        else if(valorSensor >= ((set)*0.05)+setPointAnterior && !startedTimeTr95){
                            startTr95 = System.currentTimeMillis();//inicio de contagem para 5-95%
                            startedTimeTr95 = true;
                        }
                        else if(valorSensor >= ((set)*0.95)+setPointAnterior && !endTr95){
                            Tr95 = System.currentTimeMillis() - startTr95;//final de contagem para 5-95%
                            endTr95 = true;
                        }
                        else if(valorSensor >= ((set)*0.90)+setPointAnterior && !endTr90){
                            Tr90 = System.currentTimeMillis() - startTr90;//final de contagem para 10-90%
                            endTr95 = true;
                        }
                        
                        if(valorSensor >= setPoint){//overshoot
                            if(!endTr100){
                                Tr100 = System.currentTimeMillis() - startTime;//tempo de subida 0-100%
                                endTr100 = true;
                            }
                            
                            //calculo de 2% de acomodação
                            if(valorSensor <= (setPoint*0.02)+setPoint){
                                if(!endTs2){
                                    Ts2 = System.currentTimeMillis() - startTime;
                                    endTs2 = true;
                                }
                            }
                            else{
                                endTs2 = false;
                            }
                            
                            //calculo de 5% de acomodação
                            if(valorSensor <= (setPoint*0.05)+setPoint){
                                if(!endTs5){
                                    Ts5 = System.currentTimeMillis() - startTime;
                                    endTs5 = true;
                                }
                            }
                            else{
                                endTs5 = false;
                            }
                            
                            //calculo de 10% de acomodação
                            if(valorSensor <= (setPoint*0.1)+setPoint){
                                if(!endTs2){
                                    Ts10 = System.currentTimeMillis() - startTime;
                                    endTs10 = true;
                                }
                            }
                            else{
                                endTs10 = false;
                            }
                            
                            if(valorSensor - setPoint >= Mpcm){//calculo do valor de pico
                                Mpcm = valorSensor - setPoint;
                                Mp_porcentagem = (Mpcm/setPoint)*100;
                                Tpico = System.currentTimeMillis() - startTime;//tempo do pico
                                endTpico = true;//sinalizar que acabou de calcular o tempo de pico
                            }
                        }
                    }
                    
                    else{//caso o setpoint seja abaixo do setpoint anterior
                        if(valorSensor <= setPointAnterior-(Math.abs(set)*0.1) && !startedTimeTr90){
                            startTr90 = System.currentTimeMillis();//inicio de contagem para 10-90%
                            startedTimeTr90 = true;
                        }
                        else if(valorSensor <= setPointAnterior-(Math.abs(set)*0.05) && !startedTimeTr95){
                            startTr95 = System.currentTimeMillis();//inicio de contagem para 5-95%
                            startedTimeTr95 = true;
                        }
                        else if(valorSensor <= setPointAnterior-(Math.abs(set)*0.95) && !endTr95){
                            Tr95 = System.currentTimeMillis() - startTr95;//final de contagem para 5-95%
                            endTr95 = true;
                        }
                        else if(valorSensor <= setPointAnterior-(Math.abs(set)*0.9) && !endTr90){
                            Tr90 = System.currentTimeMillis() - startTr90;//final de contagem para 10-90%
                            endTr95 = true;
                        }
                        
                        if(valorSensor <= setPoint){//undershoot
                            //tempo de subida 0-100%
                            if(!endTr100){
                                Tr100 = System.currentTimeMillis() - startTime;
                                endTr100 = true;
                            }
                            
                            //calculo de 10% de acomodação
                            if(valorSensor <= setPoint-(setPoint*0.1)){
                                if(!endTs2){
                                    Ts10 = System.currentTimeMillis() - startTime;
                                    endTs10 = true;
                                }
                            }
                            else{
                                endTs10 = false;
                            }
                            
                            //calculo de 5% de acomodação
                            if(valorSensor <= setPoint-(setPoint*0.05)){
                                if(!endTs5){
                                    Ts5 = System.currentTimeMillis() - startTime;
                                    endTs5 = true;
                                }
                            }
                            else{
                                endTs5 = false;
                            }
                            
                            //calculo de 2% de acomodação
                            if(valorSensor <= setPoint-(setPoint*0.02)){
                                if(!endTs2){
                                    Ts2 = System.currentTimeMillis() - startTime;
                                    endTs2 = true;
                                }
                            }
                            else{
                                endTs2 = false;
                            }
                        }
                    }
                    
                    if(endTpico && endTs5 && endTs10 && endTr100 && endTr95 && endTr90 && endMp){
                        if(valorSensor >= setPoint-(setPoint*0.02) && valorSensor <= setPoint+(setPoint*0.02)) 
                            break;
                    }
                    Thread.sleep(5);
                } catch (Exception ex) {
                    System.out.println("Erro durante analise de sistema: "+ex);
                }
            }
            System.out.println(_setPoint + " Tpico: "+ (double)Tpico/1000 
                                        + " Tr100%: "+(double)Tr100/1000 
                                        + " Tr95%: "+(double)Tr95/1000 
                                        + "Tr90%: "+(double)Tr90/1000
                                        + "Ts10%: "+(double)Ts10/1000
                                        + "Ts5%: "+(double)Ts5/1000
                                        + "Ts2%: "+(double)Ts2/1000
                                        + "Mp% Mp: "+Mp_porcentagem+" "+Mpcm);
        }
    }
}
