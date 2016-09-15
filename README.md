# Dynamic Water Tank Control System
This software is able to control a quanser water tank system while showing in real time the water levels of up to 7 channels and controlling up to 2 process variables. It has many techniques of PID available for choosing, such as P, PI, PD, PID and PI-D.
## Getting Started
This software is Java based and has support for all major OSes without any issues. In order to use it, just download the Control_System.zip and unpack it. There can be found the executable. 
### Offline Usage
The offline usage is available by clicking in the checkbox "offline" at login window. All the channels will be deactivated and showing the water tanks at 0, the output waves can still be visible even though they aren't being sent over the socket.
## Output Signals
By clicking on "Tipo de Função" another window opens up with all the available output waves. And by clicking on "Gerar" the selected output signal is initiated.
### Sinus Wave (Senoidal)
The first available wave is the sinus. In order to use it the parameters Amplitude, Offset and Periodo (time period in seconds) of the function must be given.
### Quadratic Wave (Quadrada)
The other available wave is the quadratic type wave. The same rules of the sinus apply here.
### Saw Wave (Dente de Serra)
This wave is of saw type. It goes from the min amplitude to the max amplitude in the given time period. The same parameters as the quadratic and sinus apply.
### Step Wave (Degrau)
This wave is just a step function, there is, just a constant output value. No offset or time period needed.
### Random Wave (Aleatória)
This is a random wave and here the mandatory parameters are: offset, time period range (min and max), amplitude range (min and max).

