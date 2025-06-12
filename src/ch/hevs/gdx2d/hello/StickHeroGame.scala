package ch.hevs.gdx2d.hello

import ch.hevs.gdx2d.components.audio.MusicPlayer
import com.badlogic.gdx.Gdx
import ch.hevs.gdx2d.components.bitmaps.BitmapImage
import ch.hevs.gdx2d.lib.GdxGraphics
import ch.hevs.gdx2d.desktop.PortableApplication
import ch.hevs.gdx2d.hello
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter


class Hero(var x: Float = 475, var y: Float = 550) {

  private var idleFrames: Array[BitmapImage] = _
  private var walkFrames: Array[BitmapImage] = _
  private var fallFrames: Array[BitmapImage] = _

  private var stateTime: Float = 0f
  private val frameDuration: Float = 0.1f

  def loadImages(): Unit = {
    idleFrames = Array(
      new BitmapImage("data/images/adventurer-idle-00.png"),
      new BitmapImage("data/images/adventurer-idle-01.png"),
      new BitmapImage("data/images/adventurer-idle-02.png"),
      new BitmapImage("data/images/adventurer-idle-03.png")
    )

    walkFrames = Array(
      new BitmapImage("data/images/adventurer-run-00.png"),
      new BitmapImage("data/images/adventurer-run-01.png"),
      new BitmapImage("data/images/adventurer-run-02.png"),
      new BitmapImage("data/images/adventurer-run-03.png"),
      new BitmapImage("data/images/adventurer-run-04.png")
    )

    fallFrames = Array(
      new BitmapImage("data/images/adventurer-fall-00.png"),
      new BitmapImage("data/images/adventurer-fall-01.png")
    )
  }

  def draw(g: GdxGraphics, game: Game): Unit = {
    stateTime += Gdx.graphics.getDeltaTime
    val currentAnimation = if (game.state == "scrolling" || game.state == "heroWalkingToDeath") walkFrames else if (game.state == "fallingHero") fallFrames else idleFrames
    val currentFrameIndex = (stateTime / frameDuration).toInt % currentAnimation.length
    val currentImage = currentAnimation(currentFrameIndex)

    val scale = 7f

    g.drawAlphaPicture(x, y-180, 0, scale, 1f, currentImage)

  }
}

class Stick(var startX: Float) {
  var length: Float = 0f
  var angle: Float = 0f

  var startY: Float = 250f
  val fallSpeed: Float = 200f
  private val growthSpeed = 600f
  val thickness = 7f
  private val rad = math.toRadians(angle)
  var centerX = startX + math.sin(rad).toFloat * length / 2
  var centerY = startY + math.cos(rad).toFloat * length / 2

  def draw(g: GdxGraphics, offsetX: Float = 0): Unit = {

    val x1 = centerX - offsetX
    val y1 = centerY

    g.drawFilledRectangle(x1, y1, thickness, length, angle)
  }

  def grow(dt: Float): Unit = {
    if (angle == 0f && length < 1250) {
      length += growthSpeed * dt
      centerY += (growthSpeed * dt)/2
    }
  }

  def fall(dt: Float, game: Game): Unit = {
    if (angle > -90f) {
      angle -= fallSpeed * dt
      centerX = thickness / 2 * math.cos(math.toRadians(angle)).toFloat - length / 2 * math.sin(math.toRadians(angle)).toFloat + startX
      centerY = thickness / 2 * math.sin(math.toRadians(angle)).toFloat + length / 2 * math.cos(math.toRadians(angle)).toFloat + startY
    }
    else {
      angle = -90
      centerY = startY
      if (centerX + length/2 >= game.platforms(2).x - game.platforms(2).width/2 && centerX + length/2 <= game.platforms(2).x + game.platforms(2).width/2){
        game.scrollTarget = game.platforms(2).x - 475f
        game.state = "scrolling"
      }
      else {
        if (centerX + length/2 <= game.platforms(2).x - game.platforms(2).width/2) game.state = "fallingStick"
        else game.state = "heroWalkingToDeath"
      }
    }
  }
}


case class Platform(x: Float = 300, y: Float = 125, width: Float = 600, height: Float = 250) {
  def draw(g: GdxGraphics, offsetX: Float): Unit ={
    g.drawFilledRectangle(x - offsetX, y, width, height, 0)
    g.drawFilledRectangle(x - offsetX, 225, width, 50, 0, Color.FOREST)
  }
}

class Game extends PortableApplication(1920, 1080) {
  private var score = 0
  private var highScore = 0

  private var background: BitmapImage = _
  private var spaceImage: BitmapImage = _
  private var youDiedImage: BitmapImage = _

  private var gameMusic: MusicPlayer = _
  private var loserMusic: MusicPlayer = _
  private var fallMusic: MusicPlayer = _

  private val minDistance = 100
  private val maxDistance = 800
  private val minWidth = 50
  private val maxWidth = 300

  var platforms: Array[Platform] = Array(
    Platform(-1000),
    Platform(),
    generateNextPlatform(Platform()),
    Platform(),
    Platform(),
    Platform()
  )

  private var hero = new Hero()
  private var stick = new Stick(platforms(1).x + (platforms(1).width/2))

  var state = "waiting"

  private var offsetX: Float = 0f
  private val scrollSpeed: Float = 600f
  var scrollTarget = 0f

  private var textF: FileHandle = _

  private var parameter1: FreeTypeFontParameter = _
  private var generator1: FreeTypeFontGenerator = _
  private var textFont: BitmapFont = _

  private var titleF: FileHandle = _

  private var parameter2: FreeTypeFontParameter = _
  private var generator2: FreeTypeFontGenerator = _
  private var titleFont: BitmapFont = _

  private var gameOverF: FileHandle = _

  private var parameter3: FreeTypeFontParameter = _
  private var generator3: FreeTypeFontGenerator = _
  private var gameOverFont: BitmapFont = _

  private var bigF: FileHandle = _

  private var parameter4: FreeTypeFontParameter = _
  private var generator4: FreeTypeFontGenerator = _
  private var bigFont: BitmapFont = _


  override def onInit(): Unit ={
    setTitle("Stick Hero")

    gameMusic = new MusicPlayer("data/music/ytmp3free.cc_undertale-ost-014-heartache-youtubemp3free.org.mp3")
    loserMusic = new MusicPlayer("data/music/ytmp3free.cc_the-price-is-right-losing-horn-sound-effect-hd-youtubemp3free.org.mp3")
    fallMusic = new MusicPlayer("data/music/ytmp3free.cc_mario-fall-waa-sound-effect-hd-youtubemp3free.org.mp3")

    fallMusic.setVolume(0.1f)
    loserMusic.setVolume(0.1f)

    textF = Gdx.files.internal("data/fonts/Game Paused DEMO.otf")

    parameter1 = new FreeTypeFontParameter()
    generator1 = new FreeTypeFontGenerator(textF)
    parameter1.size = generator1.scaleForPixelHeight(40)
    textFont = generator1.generateFont(parameter1)

    titleF = Gdx.files.internal("data/fonts/RushDriver-Italic.otf")

    parameter2 = new FreeTypeFontParameter()
    generator2 = new FreeTypeFontGenerator(textF)
    parameter2.size = generator2.scaleForPixelHeight(200)
    parameter2.color = Color.BLACK
    titleFont = generator2.generateFont(parameter2)

    gameOverF = Gdx.files.internal("data/fonts/Death World Free Trial.ttf")

    parameter3 = new FreeTypeFontParameter()
    generator3 = new FreeTypeFontGenerator(gameOverF)
    parameter3.size = generator3.scaleForPixelHeight(200)
    parameter3.color = Color.RED
    gameOverFont = generator3.generateFont(parameter3)

    bigF = Gdx.files.internal("data/fonts/Death World Free Trial.ttf")

    parameter4 = new FreeTypeFontParameter()
    generator4 = new FreeTypeFontGenerator(bigF)
    parameter4.size = generator4.scaleForPixelHeight(40)
    parameter4.color = Color.WHITE
    bigFont = generator4.generateFont(parameter4)

    background = new BitmapImage("data/images/skyWithClouds.jpg")
    spaceImage = new BitmapImage("data/images/spacebar.png")
    youDiedImage = new BitmapImage("data/images/6ethIzc.png")
    hero.loadImages()
    platforms(2) = generateNextPlatform(platforms(1))
    platforms(3) = generateNextPlatform(platforms(2))
    platforms(4) = generateNextPlatform(platforms(3))
    platforms(5) = generateNextPlatform(platforms(4))

    gameMusic.loop()
  }

  private def restart(): Unit ={
    loserMusic.stop()
    fallMusic.stop()
    state = "waiting"
    offsetX = 0
    highScore = if(score > highScore) score else highScore
    score = 0
    stick = new Stick(600)
    hero = new Hero()
    hero.loadImages()
    scrollTarget = 0
    platforms(0) = Platform(-1000)
    platforms(1) = Platform()
    platforms(2) = generateNextPlatform(platforms(1))
    platforms(3) = generateNextPlatform(platforms(2))
    platforms(4) = generateNextPlatform(platforms(3))
    platforms(5) = generateNextPlatform(platforms(4))
    gameMusic.loop()
  }

  override def onGraphicRender(g: GdxGraphics): Unit ={
    val dt = Gdx.graphics.getDeltaTime

    state match {
      case "growing" => stick.grow(dt)
      case "falling" => stick.fall(dt, this)
      case "scrolling" => updateScroll(dt)
      case "gameover" => gameOver(g)
      case "fallingStick" => fallingStick(dt)
      case "heroWalkingToDeath" => heroWalkingToDeath(dt)
      case "fallingHero" => fallingHero(dt)
      case _ =>
    }

    g.clear()
    g.drawAlphaPicture(1000, 750, 0, 0.35f, 7000, background)

    g.setColor(Color.BROWN)
    platforms.foreach(_.draw(g, offsetX))

    g.setColor(Color.BLACK)
    stick.draw(g, offsetX)

    g.setColor(Color.BLUE)
    hero.draw(g, this)

    if(state == "waiting" && platforms(1) == Platform()){
      g.drawFilledRectangle(210, 615, 100, 25, 0, Color.WHITE)
      g.drawString(20, 635, "press", textFont)
      g.drawAlphaPicture(210, 615, 0, 0.20f, 1, spaceImage)
      g.drawString(290, 635, "to start growing your Stick", textFont)
      g.drawString(495, 1000, "Stick Hero", titleFont)
    }
    else{
      g.drawString(1600, 1050, s"Score $score", bigFont)
      //g.drawString(1700, 1050, s"$score", bigFont)
      g.drawString(1600, 1000, s"High Score $highScore", bigFont)
      //g.drawString(1750, 1000, s"$highScore", bigFont)
    }

    if(state == "gameover") gameOver(g)
    g.drawFPS()
  }

  override def onKeyDown(keycode: Int): Unit ={
    if (keycode == Keys.SPACE && state == "waiting") {
      state = "growing"
      println("[KEYDOWN] passage à growing")
    }
    if (keycode == Keys.ENTER && state == "gameover") {
      state = "waiting"
      restart()
    }
  }

  override def onKeyUp(keycode: Int): Unit ={
    if (keycode == Keys.SPACE && state == "growing") {
      state = "falling"
      println("[KEYUP] passage à falling")
    }
  }

  private def updateScroll(dt: Float): Unit = {
    val scrollAmount = scrollSpeed * dt
    offsetX += scrollAmount

    if(offsetX >= scrollTarget) {
      offsetX = scrollTarget
      finalizeScroll()
    }
  }

  private def finalizeScroll(): Unit ={
    val scrollDistance = scrollTarget

    platforms = platforms.map(p => p.copy(x = p.x - scrollDistance))
    stick.startX -= scrollDistance
    offsetX = 0f

    score += 1

    platforms = Array(
      platforms(1),
      platforms(2),
      platforms(3),
      platforms(4),
      platforms(5),
      generateNextPlatform(platforms(5))
    )

    stick = new Stick(platforms(1).x + platforms(1).width / 2)

    state = "waiting"
  }

  private def fallingStick(dt: Float): Unit ={
    if (stick.angle > -180f) {
      stick.angle -= stick.fallSpeed * dt
      stick.centerX = stick.thickness / 2 * math.cos(math.toRadians(stick.angle)).toFloat - stick.length / 2 * math.sin(math.toRadians(stick.angle)).toFloat + stick.startX
      stick.centerY = stick.thickness / 2 * math.sin(math.toRadians(stick.angle)).toFloat + stick.length / 2 * math.cos(math.toRadians(stick.angle)).toFloat + stick.startY
    }
    else {
      stick.angle = -180f
      stick.centerX= stick.startX
      state = "gameover"
    }
  }

  private def heroWalkingToDeath(dt: Float): Unit ={
    val speed = 600 * dt
    hero.x += speed
    val stickEnd = stick.startX + stick.length

    if (hero.x >= stickEnd) {
      state = "fallingHero"
    }
  }

  private def fallingHero(dt: Float): Unit ={
    gameMusic.stop()
    fallMusic.play()
    hero.y -= 600 * dt
    if (hero.y < 0) {
      state = "gameover"
    }
  }

  private def gameOver(g: GdxGraphics): Unit ={
    if (hero.y < 450){
      g.drawString(620, 750, "You Died", gameOverFont)
      g.drawString(735, 525, "Press ENTER to play again", textFont)
    }
    else{
      g.drawString(620, 750, "You Lost", gameOverFont)
      g.drawString(735, 525, "Press ENTER to play again", textFont)
      gameMusic.stop()
      loserMusic.play()
    }
  }

  private def generateNextPlatform(prev: Platform): Platform ={
    val distance = minDistance + scala.util.Random.nextFloat() * (maxDistance - minDistance)
    val width = minWidth + scala.util.Random.nextFloat() * (maxWidth - minWidth)
    val x = prev.x + prev.width + distance
    Platform(x, prev.y, width)
  }
}

object StickHeroGame extends App {
  new Game
}
