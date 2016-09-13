/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sistemacontrole;

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
        double leituraCanal = this.leituraEscrita.getCanalLeitura(0);
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
        this.setPoint = setPoint;
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
//        
        if(this.I > 4){
            this.I = 4;
        }
        else if(this.I < -4){
            this.I = -4;
        }
    }
    
//    //checar travas para aplicar o wind up
//    public double travasWindUp(double tensao){
//        if(tensao > 4){
//            return 4;
//        }
//        else if(tensao < -4){
//            return -4;
//        }
//        else{
//            return tensao;
//        }
//    }
        
}
