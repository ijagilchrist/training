package org.training.simulate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SimulationApplication {

    public static void main(String[] args) throws ClassNotFoundException {
        
        String utility = args[0];
        String[] utilityArgs = new String[args.length-1];
        System.arraycopy(args,1,utilityArgs,0,utilityArgs.length);
        
        String utilityClassName = "org.training.simulate.command."+utility;
        Class<?> utilityClass = Class.forName(utilityClassName);
        
        SpringApplication.run(utilityClass,utilityArgs);

    }
    
}
