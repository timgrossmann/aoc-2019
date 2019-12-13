import { Computer } from "./Computer";
import { Panel } from "./Panel";





let computer = Computer.load("input.txt");


let panel = new Panel();

let x = 0;
while(!computer.isDone())
{
    let c = panel.getColor();
    computer.setInput(c);
    while(computer.run());
    let output = computer.getOutput();
    console.log(output[x]+","+output[x+1]);
    panel.setColor(output[x++]);
    panel.turn(output[x++]);
    panel.moveForward();
}
console.log("My puzzle answer is "+panel.countPanels());




