import processing.core.*; 
import processing.xml.*; 

import java.applet.*; 
import java.awt.Dimension; 
import java.awt.Frame; 
import java.awt.event.MouseEvent; 
import java.awt.event.KeyEvent; 
import java.awt.event.FocusEvent; 
import java.awt.Image; 
import java.io.*; 
import java.net.*; 
import java.text.*; 
import java.util.*; 
import java.util.zip.*; 
import java.util.regex.*; 

public class DynamicDust extends PApplet {



//Dust flow simulation?

float max_clean;

class Cell
{
  float dustLevel;
  boolean exists;
  
  Cell(float dustLev, boolean ex)
  {
    dustLevel = dustLev;
    exists = ex;
  }
  
  Cell(Cell other)
  {
    dustLevel = other.dustLevel;
    exists = other.exists;
  }
}

int XSIZE = 100;
int YSIZE = 100;

Cell[][] currentCells;
Cell[][] nextCells;
int generation;

public void initCells()
{
  for(int x = 0; x < XSIZE; x++)
  {
    for(int y = 0; y < YSIZE; y++)
    {
      float inf = max_clean;
      boolean ex = random(1) < 0.9f;
      Cell c = new Cell(inf, ex);
      currentCells[x][y] = new Cell(c);
      nextCells[x][y] = new Cell(c);
    } 
  }
}

public void copyCells()
{
  for(int x = 0; x < XSIZE; x++)
  {
    for(int y = 0; y < YSIZE; y++)
    {
      currentCells[x][y] = new Cell(nextCells[x][y]);
    } 
  }
}

public void calculateGeneration()
{
  
  for(int x = 0; x < XSIZE; x++)
  {
    for(int y = 0; y < YSIZE; y++)
    {
      Cell current = currentCells[x][y];
      if(current.exists)
      {
        if(current.dustLevel < EPSILON)
        {
          nextCells[x][y].exists = false;
        }
        else
        {
          //dx_i/dt
           // = -\sum_j (x_i - x_j)
           // + dust_source_rate_i - clean_factor_rate_i
           
           //apply DANIEL THEORY
           //float neighboringDustDiff = 0;
           float transferFactor = 4.8f;
           float newdust = current.dustLevel;
           for(int i = 0; i < 8; i++)
           {
             Cell cneighbor = getCurrentCellAt(x, y, i);
             if(cneighbor.exists)
             {
               float neighboringDustDiff =
                 (current.dustLevel - cneighbor.dustLevel) / transferFactor;
               newdust -= neighboringDustDiff;
               //c.dustLevel += neighboringDustDiff;
             }
           }
           
           float extraDust = random(1) < (1 / 100.0f) ? 2 : 0;
           float cleaning = random(1) < (1 / 500.0f) ? 0 : 0;
           
           newdust += -extraDust + cleaning;
           //newdust += 1;
           newdust = constrain(newdust, 0, max_clean);
           getNextCellAt(x, y).dustLevel = newdust;

        }
        
//        //DO
//        if(current.infection == max_health)
//        {
//          //should infect?
//          if(random(1) < (300 / 100000.0f))
//          {
//            //infect this cell!!
//            nextCells[x][y].infection --;
//          }
//          else
//          {
//            //perhaps multiply?
//            if(random(1) < (3 / 100.f))
//            {
//              //put a cell in a random adjacent empty spot
//              int tried = 0;
//              int randstart = (int) random(0, 8);
//              boolean found = false;
//              while(tried < 8 && !found)
//              {
//                if(!cellExistsAroundCell(x, y, tried + randstart))
//                {
//                  putNewCellAt(x, y, tried + randstart);
//                  found = true;
//                }
//                tried++;
//              } //finding spot for new cell
//            } //reproduce
//          } //not infected 
//        } //healthy cell
//        else
//        {
//          //not healthy cell
//          if(current.infection > 1)
//          {
//            nextCells[x][y].infection--;
//          }
//          else
//          {
//            nextCells[x][y].exists = false;
//            if(random(1) < (30/100.0f))
//            {
//              //infect surrounding cells
//              for(int i = 0; i < 8; i++)
//              {
//                Cell c = getNextCellAt(x, y, i);
//                if(c.exists && c.infection > 1)
//                {
//                  c.infection--;
//                }
//              }
//            }
//          }
//        }
      } //cell exists
    } //for y
  } //for x
  
  copyCells();
}

public void putNewCellAt(int x, int y, int index)
{
  Cell c = getNextCellAt(x, y, index);
  c.dustLevel = max_clean;
  c.exists = true;
}

public void getIndexCoordsAt(int x, int y, int index, int[] xy)
{
  index = index % 8;
  int targetx = x, targety = y;
  switch(index)
  {
    case 0:
      targetx --;
      break;
    case 1:
      targetx --;
      targety --;
      break;
    case 2:
      targety --;
      break;
    case 3:
      targety--;
      targetx++;
      break;
     case 4:
       targetx++;
       break;
     case 5:
       targetx++;
       targety++;
       break;
     case 6:
       targety++;
       break;
     case 7:
       targety++;
       targetx--;
       break;
  }
  
  xy[0] = targetx;
  xy[1] = targety;
}

public Cell getNextCellAt(int x, int y, int index)
{
  int[] xy = new int[2];
  getIndexCoordsAt(x, y, index, xy);
  Cell c = getNextCellAt(xy[0], xy[1]);
  return c;
}

public Cell getCurrentCellAt(int x, int y, int index)
{
  int[] xy = new int[2];
  getIndexCoordsAt(x, y, index, xy);
  Cell c = getCurrentCellAt(xy[0], xy[1]);
  return c;
}

public boolean cellExistsAroundCell(int x, int y, int index)
{
  Cell c = getNextCellAt(x, y, index);
  return c.exists;
}

public Cell getNextCellAt(int x, int y)
{
  x = (x + XSIZE) % XSIZE;
  y = (y + YSIZE) % YSIZE;
  return nextCells[x][y];
}

public Cell getCurrentCellAt(int x, int y)
{
  x = (x + XSIZE) % XSIZE;
  y = (y + YSIZE) % YSIZE;
  return currentCells[x][y];
}

public void update()
{
  //if(frameCount % 60 == 0)
  {
    //next generation
    calculateGeneration();
  }
}

public void drawCells()
{
  //sizes
  float wid = width / ((float)XSIZE);
  float hei = height / ((float)YSIZE);
    
  for(int x = 0; x < XSIZE; x++)
  {
    for(int y = 0; y < YSIZE; y++)
    {
      Cell current = currentCells[x][y];
      if(current.exists)
      {
        int thegray = PApplet.parseInt( 255 / max_clean);
        ellipseMode(CORNER);
        noStroke();
        fill(PApplet.parseInt(thegray * current.dustLevel));
        float factor = 0.8f;
        ellipse(x*wid, y*hei, wid * factor, hei * factor);
      }
    } 
  }
}

public void setup () 
{
  size(500, 500);
  smooth();
  generation = 0;
  
  currentCells = new Cell[XSIZE][YSIZE];
  nextCells = new Cell[XSIZE][YSIZE];
  max_clean = 10;

  //make cells  
  initCells();
}

public void draw ()
{
  background(0);
  
  update();
  
  drawCells();
  
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "--bgcolor=#F0F0F0", "DynamicDust" });
  }
}
