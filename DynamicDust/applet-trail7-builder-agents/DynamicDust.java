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

boolean shouldOutput = false;

//Dust flow simulation?

float max_clean = 10;
float over_clean = 200;
int cleaned = 0;
int deathed = 0;

int XSIZE = 100;
int YSIZE = XSIZE;

PVector builderLocation = new PVector(XSIZE/4, 3 * YSIZE / 4);

int maxBuilders = 10;

//dust dynamics
float transferFactor = 50;
float baseDustCleaningChance = 500.0f;
float baseDustGenerationChance = 18.0f;

//extras
  PrintWriter dataWriter;
int livecells = 0;

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

public int sign(int in)
{
  return in / abs(in);
}

class Builder
{
  boolean active; //is currently moving/building
  PVector location;
  PVector targetLocation;
  boolean goingOut; //going to the site or returning to base?
  
  int movementSpeed;
  int  generationsWandering;
  int generationsToBuild;
  
  Builder()
  {
    active = false;
    goingOut = true;
    location = new PVector(0,0);
    targetLocation = new PVector(0,0);
    movementSpeed = 20;
    generationsWandering = 0;
    generationsToBuild = 1;
  }
  
  public void activate(PVector newTarget)
  {
    active = true;
    goingOut = true;
    targetLocation = newTarget;
    location = new PVector(builderLocation.x, builderLocation.y);
    //affect speed according to time spent wandering
    movementSpeed = PApplet.parseInt(20 * 100 / ((float)(generationsWandering + 10)));
    movementSpeed = constrain(movementSpeed, 1, 20);
  }
  
  public void update()
  {
    if(!active)
      return;
    
    generationsWandering++;
    
    PVector actualTarget;
      
    //for now, move gridwise, without respecting dead cells.
    if(goingOut)
    {
      //move to site
      actualTarget = new PVector(targetLocation.x, targetLocation.y);
    }
    else
    {
      //back to base
      actualTarget = new PVector(builderLocation.x, builderLocation.y);
    }
    
    //update location according to speed
    //choose furthest distance (x,y)
    PVector movementVector = new PVector(actualTarget.x, actualTarget.y);
    movementVector.sub(location);
    if(abs(movementVector.x) > abs(movementVector.y))
    {
      if(movementSpeed >= abs(movementVector.x))
      {
        location.x += movementVector.x;
      }
      else
      {
        location.x += sign(PApplet.parseInt(movementVector.x)) * movementSpeed;
      }
    }
    else
    {
      if(movementSpeed >= abs(movementVector.y))
      {
        location.y += movementVector.y;
      }
      else
      {
        location.y += sign(PApplet.parseInt(movementVector.y)) * movementSpeed;
      }
    }
    
    //check if reached destination
    if(location.x == actualTarget.x && location.y == actualTarget.y)
    {
      if(goingOut)
      {
        //got to dusty location!
        //activate CLEAN
        Cell cellToClean = getNextCellAt(PApplet.parseInt(location.x), PApplet.parseInt(location.y));
        if(cellToClean.exists)
        {
          //only clean alive cells
          cleaned++;
          cellToClean.dustLevel = over_clean;
        }
        //go back to depot now
        goingOut = false;
      }
      else
      {
        active = false;
      }
    }
  }
}

public void launchBuilder(int x, int y)
{
  //look for an available builder
  //launch it towards the dusty area
  int i = 0;
  while(i < maxBuilders && builders[i].active)
  {
    i++;
  }
  
  if(i == maxBuilders)
  {
    println("Out of builders!!!!");
    return; //none available
  }
    
  builders[i].activate(new PVector(x, y));
}

public void updateBuilders()
{
  for(int i = 0; i < maxBuilders; i++)
  {
    builders[i].update();
  }
}

Builder[] builders;
Cell[][] currentCells;
Cell[][] nextCells;
int generation;

public void initBuilders()
{
  builders = new Builder[maxBuilders];
  
  for(int i = 0; i < maxBuilders; i++)
  {
    builders[i] = new Builder();
  }
}

public void initCells()
{
  //image
  PImage mapReading;
  mapReading = loadImage("Map-Trial-1.png");
  int imgwid = mapReading.width;
  int imghei = mapReading.height;
  float blockx = imgwid / ((float) XSIZE);
  float blocky = imghei / ((float)YSIZE);
  mapReading.loadPixels();
  
  for(int x = 0; x < XSIZE; x++)
  {
    for(int y = 0; y < YSIZE; y++)
    {
      float inf = max_clean;
      int px_x = PApplet.parseInt((x + 0.5f) * blockx);
      int px_y = PApplet.parseInt((y + 0.5f) * blocky);
      int col = mapReading.pixels[px_x + imgwid * px_y];
      boolean ex = brightness(col) > 0.5f;
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
  generation ++;
  livecells = 0;
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

           float cleaning = 0;
           //increase chance of cleaning dusty cell
           if(random(1) < (1 / (baseDustCleaningChance * pow(newdust, 2))))
           {
             launchBuilder(x, y);
             //cleaning = over_clean;
             //cleaned++;
           }
           
           //newdust += -extraDust + cleaning;
           newdust += cleaning;
           newdust = constrain(newdust, 0, over_clean);
           getNextCellAt(x, y).dustLevel = newdust;

        }
      } //cell exists
    } //for y
  } //for x
  
  copyCells();
  
  for(int x = 0; x < XSIZE; x++)
  {
    for(int y = 0; y < YSIZE; y++)
    {
      Cell current = currentCells[x][y];
      if(!current.exists)
      {
        //empty cell
        //iterate neighbors ...
        int randadd = PApplet.parseInt(random(0, 8));
        for(int i = 0; i < 8; i++)
        {
          if(random(1) < (1/baseDustGenerationChance))
          {
            Cell c = getNextCellAt(x, y, i + randadd);
            if(c.exists)
            {
              c.dustLevel -= 0.5f;
              c.dustLevel = constrain(c.dustLevel, 0, over_clean);
            }
          }
        }
      }
      else //live cell
      {
        livecells ++;
      }
    }
  }
  
  updateBuilders();
  
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
  if((x < 0 || x >= XSIZE)
    || (y < 0 || y >= XSIZE))
    {
      return new Cell(max_clean, true);
    }
  
  //x = (x + XSIZE) % XSIZE;
  //y = (y + YSIZE) % YSIZE;
  return nextCells[x][y];
}

public Cell getCurrentCellAt(int x, int y)
{
    if((x < 0 || x >= XSIZE)
    || (y < 0 || y >= XSIZE))
    {
      return new Cell(max_clean, true);
    }
  
  
  //x = (x + XSIZE) % XSIZE;
  //y = (y + YSIZE) % YSIZE;
  return currentCells[x][y];
}

public void update()
{
  calculateGeneration();
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
        if(current.dustLevel > max_clean)
        {
          fill(color(0,0,255));
        }
        else
        {
          fill(PApplet.parseInt(thegray * current.dustLevel));
        }
        float factor = 0.8f;
        ellipse(x*wid, y*hei, wid * factor, hei * factor);
      }
    } 
  }
}

public void drawBuilders()
{
  noFill();
  rectMode(CENTER);
  
  strokeWeight(3);
  stroke(20);
  rect(builderLocation.x * width / XSIZE, builderLocation.y * height / YSIZE,
        10, 10);

  strokeWeight(1);
  stroke(color(50, 100, 50));
  rect(builderLocation.x * width / XSIZE, builderLocation.y * height / YSIZE,
        10, 10);
        
        
   for(int i = 0; i < maxBuilders; i++)
   {
     if(builders[i].active)
     {
       strokeWeight(2);
       stroke(color(255, 255, 0));
       rect(builders[i].location.x * width / XSIZE, builders[i].location.y * height / YSIZE,
        5, 5);
     }
   }
}

public void setup () 
{
  size(500, 500);
  smooth();
  
  if(shouldOutput)
  {
    dataWriter = createWriter("data_" + year() + "-" + month() + "-" + day() + "_" + hour() + "-" + minute() + "-" + second() +  ".txt");
    dataWriter.println("Generation\tLiveCells\tCleanedCells");
  }
  
  generation = 0;
  
  currentCells = new Cell[XSIZE][YSIZE];
  nextCells = new Cell[XSIZE][YSIZE];


  //make cells  
  initCells();
  
  //make builders
  initBuilders();
}

public void draw ()
{
  background(0);
  
  update();
  
  drawCells();
  
  drawBuilders();
 
 if(shouldOutput)
 {
  dataWriter.println("" 
        + generation
        + "\t" + livecells
        + "\t" + cleaned);
 }
  
  if(frameCount % 60 == 0)
  {
    
    if(shouldOutput)
    {
      dataWriter.flush();
    }
    
    int newDeathed = XSIZE * YSIZE - livecells;
    int diffDeathed = newDeathed - deathed;
    deathed = newDeathed;
    
    println("**************\r\n"
      + "Generations: " + generation + ", cleaned: " + cleaned
      + "\r\n"
      + "cleaning per gen: " + (cleaned / ((float)generation))
      + ", live cells: " + livecells
//      + ", cleaning per gen per live cell: " + (cleaned / ((float)generation * livecells))
      + "\r\n"
      + "New dead: " + diffDeathed
      + ", Dead per generation: " + newDeathed / ((float)generation));
  }
}
  static public void main(String args[]) {
    PApplet.main(new String[] { "--bgcolor=#F0F0F0", "DynamicDust" });
  }
}
