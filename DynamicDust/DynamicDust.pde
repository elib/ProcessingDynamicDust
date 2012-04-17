

//Dust flow simulation?

float max_clean = 10;
float over_clean = 200;
int cleaned = 0;
int deathed = 0;

int XSIZE = 100;
int YSIZE = XSIZE;

PVector builderLocation = new PVector(XSIZE/4, 3 * YSIZE / 4);

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

Cell[][] currentCells;
Cell[][] nextCells;
int generation;

void initCells()
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
      int px_x = int((x + 0.5) * blockx);
      int px_y = int((y + 0.5) * blocky);
      color col = mapReading.pixels[px_x + imgwid * px_y];
      boolean ex = brightness(col) > 0.5;
      Cell c = new Cell(inf, ex);
      currentCells[x][y] = new Cell(c);
      nextCells[x][y] = new Cell(c);
    } 
  }
}

void copyCells()
{
  for(int x = 0; x < XSIZE; x++)
  {
    for(int y = 0; y < YSIZE; y++)
    {
      currentCells[x][y] = new Cell(nextCells[x][y]);
    } 
  }
}

void calculateGeneration()
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
             cleaning = over_clean;
             cleaned++;
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
        int randadd = int(random(0, 8));
        for(int i = 0; i < 8; i++)
        {
          if(random(1) < (1/baseDustGenerationChance))
          {
            Cell c = getNextCellAt(x, y, i + randadd);
            if(c.exists)
            {
              c.dustLevel -= 0.5;
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
  
  copyCells();
}

void putNewCellAt(int x, int y, int index)
{
  Cell c = getNextCellAt(x, y, index);
  c.dustLevel = max_clean;
  c.exists = true;
}

void getIndexCoordsAt(int x, int y, int index, int[] xy)
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

Cell getNextCellAt(int x, int y, int index)
{
  int[] xy = new int[2];
  getIndexCoordsAt(x, y, index, xy);
  Cell c = getNextCellAt(xy[0], xy[1]);
  return c;
}

Cell getCurrentCellAt(int x, int y, int index)
{
  int[] xy = new int[2];
  getIndexCoordsAt(x, y, index, xy);
  Cell c = getCurrentCellAt(xy[0], xy[1]);
  return c;
}

boolean cellExistsAroundCell(int x, int y, int index)
{
  Cell c = getNextCellAt(x, y, index);
  return c.exists;
}

Cell getNextCellAt(int x, int y)
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

Cell getCurrentCellAt(int x, int y)
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

void update()
{
  //if(frameCount % 60 == 0)
  {
    //next generation
    calculateGeneration();
  }
}

void drawCells()
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
        int thegray = int( 255 / max_clean);
        
        ellipseMode(CORNER);
        noStroke();
        if(current.dustLevel > max_clean)
        {
          fill(color(0,0,255));
        }
        else
        {
          fill(int(thegray * current.dustLevel));
        }
        float factor = 0.8;
        ellipse(x*wid, y*hei, wid * factor, hei * factor);
      }
    } 
  }
}

void drawBuilders()
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
}

void setup () 
{
  size(500, 500);
  smooth();
  
  dataWriter = createWriter("data_" + year() + "-" + month() + "-" + day() + "_" + hour() + "-" + minute() + "-" + second() +  ".txt");
  dataWriter.println("Generation\tLiveCells\tCleanedCells");
  
  generation = 0;
  
  currentCells = new Cell[XSIZE][YSIZE];
  nextCells = new Cell[XSIZE][YSIZE];


  //make cells  
  initCells();
}

void draw ()
{
  background(0);
  
  update();
  
  drawCells();
  
  drawBuilders();
  
  dataWriter.println("" 
        + generation
        + "\t" + livecells
        + "\t" + cleaned);
  
  if(frameCount % 60 == 0)
  {
    
    dataWriter.flush();
    
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
