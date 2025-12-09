import korlibs.audio.sound.*
import korlibs.image.bitmap.*
import korlibs.image.format.*
import korlibs.io.file.std.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.korge.view.collision.*
import korlibs.time.*
import kotlin.random.Random

/**
 * Base class for Flappy Bird scenes that contains common functionality
 * shared between the game and training scenes.
 */
abstract class BaseFlappyBirdScene : Scene() {

    // Constants
    protected val gravity = 1000.0
    protected val gap = 100
    protected val birdStartX = 100.0
    protected val birdStartY = 256.0

    // Resources
    protected lateinit var bluebirdAnimation: SpriteAnimation
    protected lateinit var pipeImage: Bitmap
    protected lateinit var baseImage: Bitmap
    protected lateinit var hitSound: Sound
    protected lateinit var pointSound: Sound

    // Ground elements
    protected lateinit var base1: Image
    protected lateinit var base2: Image

    // Game elements
    protected val pipes = mutableListOf<View>()

    /**
     * Initialize common resources used in all FlappyBird scenes
     */
    protected suspend fun initResources() {
        val bluebirdSpriteMap = resourcesVfs["bluebird.png"].readBitmap()
        bluebirdAnimation = SpriteAnimation(
            spriteMap = bluebirdSpriteMap,
            spriteWidth = 34,
            spriteHeight = 24,
            columns = 4,
            rows = 1
        )

        pipeImage = resourcesVfs["pipe-green.png"].readBitmap()
        baseImage = resourcesVfs["base.png"].readBitmap()
        hitSound = resourcesVfs["hit.wav"].readSound()
        pointSound = resourcesVfs["point.wav"].readSound()
    }

    /**
     * Initialize and set up the ground/base
     */
    protected fun Container.initGround() {
        base1 = image(baseImage) {
            position(0, 400)
        }
        base2 = image(baseImage) {
            position(288, 400)
        }
    }

    /**
     * Bring ground elements to the top of the display order
     */
    protected fun bringGroundToTop() {
        base1.bringToTop()
        base2.bringToTop()
    }

    /**
     * Creates a new pair of pipes at the given position
     */
    protected fun Container.createPipePair(x: Double, middleY: Double): Pair<View, View> {
        val topPipe = image(pipeImage) {
            scaleY = -1.0
            anchor(0.5, 0.0)
            position(x, middleY - gap / 2)
        }

        val bottomPipe = image(pipeImage) {
            anchor(0.5, 0.0)
            position(x, middleY + gap / 2)
        }

        pipes.add(topPipe)
        pipes.add(bottomPipe)

        return Pair(topPipe, bottomPipe)
    }

    /**
     * Checks if a sprite collides with any pipe
     */
    protected fun checkPipeCollision(sprite: Sprite): Boolean {
        for (pipe in pipes) {
            if (sprite.collidesWith(pipe)) {
                return true
            }
        }
        return false
    }

    /**
     * Creates a fixed updater that adds pipes at regular intervals
     */
    protected fun Container.setupPipeSpawner(
        interval: TimeSpan = 1.0.seconds,
        spawnPipes: (Double) -> Unit
    ) {
        addFixedUpdater(time = interval) {
            val middle = Random.nextDouble(175.0, 295.0)
            spawnPipes(middle)
        }
    }

    /**
     * Updates pipe positions by moving them to the left
     */
    protected fun updatePipes(speed: Double = 2.0) {
        pipes.forEach { it.x -= speed }
        pipes.removeAll { it.x < -64 }
    }
}
