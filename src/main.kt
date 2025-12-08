import korlibs.audio.sound.*
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.io.file.std.*
import korlibs.korge.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.korge.view.collision.*
import korlibs.math.geom.*
import korlibs.time.*
import kotlin.random.*
import kotlinx.coroutines.launch
import korlibs.korge.input.keys

suspend fun main() = Korge(windowSize = Size(512, 512), backgroundColor = Colors["#2b2b2b"]) {
    val sceneContainer = sceneContainer()
    sceneContainer.changeTo { provideScene() }
}

class GameScene : Scene() {
    enum class GameState {
        Ready, Running, GameOver
    }

    override suspend fun SContainer.sceneMain() {
        var gameState = GameState.Ready
        setBackground()

        val bluebirdSpriteMap = resourcesVfs["bluebird.png"].readBitmap()

        val bluebirdAnimation = SpriteAnimation(
            spriteMap = bluebirdSpriteMap,
            spriteWidth = 34,
            spriteHeight = 24,
            columns = 4,
            rows = 1
        )

        val bird = sprite(bluebirdAnimation) {
            position(100, 256)
        }

        bird.playAnimationLooped(spriteDisplayTime = 100.milliseconds)

        var velocity = 0.0
        val gravity = 1000.0

        val hitSound = resourcesVfs["hit.wav"].readSound()
        val flapSound = resourcesVfs["wing.wav"].readSound()
        val pointSound = resourcesVfs["point.wav"].readSound()

        var score = 0
        val scoreText = text("Score: $score") {
            position(10, 10)
        }

        val pipes = mutableListOf<View>()
        val passedPipes = mutableSetOf<View>()

        val pipeImage = resourcesVfs["pipe-green.png"].readBitmap()
        val baseImage = resourcesVfs["base.png"].readBitmap()
        val base1 = image(baseImage) {
            position(0, 400)
        }
        val base2 = image(baseImage) {
            position(288, 400)
        }

        val gameOverImage = resourcesVfs["gameover.png"].readBitmap()
        val gameOverView = image(gameOverImage) {
            anchor(0.5, 0.5)
            position(256, 256)
            visible = false
        }

        val messageImage = resourcesVfs["message.png"].readBitmap()
        val messageView = image(messageImage) {
            anchor(0.5, 0.5)
            position(256, 256)
            visible = true
        }
        messageView.bringToTop()

        var isAiPlaying = false
        val aiStatusText = text(if (isAiPlaying) "AI Playing" else "Manual") {
            position(400, 10)
        }

        keys {
            down {
                if (it.key == korlibs.event.Key.A) {
                    isAiPlaying = !isAiPlaying
                    aiStatusText.text = if (isAiPlaying) "AI Playing" else "Manual"
                }

                if (it.key == korlibs.event.Key.SPACE) {
                    if (!isAiPlaying) {
                        when (gameState) {
                            GameState.Ready -> {
                                gameState = GameState.Running
                                messageView.visible = false
                                velocity = -250.0
                                launch { flapSound.play() }
                            }
                            GameState.Running -> {
                                velocity = -250.0
                                launch { flapSound.play() }
                            }
                            GameState.GameOver -> {
                                launch {
                                    sceneContainer.changeTo { GameScene() }
                                }
                            }
                        }
                    }
                }
            }
        }

        onClick {
            if (!isAiPlaying) {
                when (gameState) {
                    GameState.Ready -> {
                        gameState = GameState.Running
                        messageView.visible = false
                        velocity = -250.0
                        launch { flapSound.play() }
                    }
                    GameState.Running -> {
                        velocity = -250.0
                        launch { flapSound.play() }
                    }
                    GameState.GameOver -> {
                        launch {
                            sceneContainer.changeTo { GameScene() }
                        }
                    }
                }
            }
        }

        addUpdater { dt ->
            if (gameState == GameState.Running || gameState == GameState.GameOver) {
                velocity += gravity * dt.seconds
                bird.y += velocity * dt.seconds

                if (bird.y > 375) {
                    bird.y = 375.0
                    velocity = 0.0
                }

                if (bird.y < 0) {
                    bird.y = 0.0
                }
            }
        }

        addFixedUpdater(time = 1.0.seconds) {
            if (gameState == GameState.Running) {
                val gap = 100
                val middle = Random.nextDouble(175.0, 295.0)
                val topPipe = image(pipeImage) {
                    scaleY = -1.0
                    anchor(0.5, 0.0)
                    position(512, middle - gap / 2)
                }
                val bottomPipe = image(pipeImage) {
                    anchor(0.5, 0.0)
                    position(512, middle + gap / 2)
                }
                pipes.add(topPipe)
                pipes.add(bottomPipe)
            }
        }

        addUpdater { dt ->
            if (gameState == GameState.Running) {
                if (isAiPlaying) {
                    val nextPipe = pipes.firstOrNull { it.x + it.width > bird.x }
                    if (nextPipe != null) {
                        val gap = 150
                        val pipeCenter = nextPipe.y + gap / 2
                        if (bird.y > pipeCenter) {
                            velocity = -250.0
                        }
                    }
                }

                pipes.forEach { pipe ->
                    pipe.x -= 2.0
                }

                for (i in pipes.indices step 2) {
                    val topPipe = pipes[i]
                    if (topPipe.x + topPipe.width < bird.x && !passedPipes.contains(topPipe)) {
                        launch { pointSound.play() }
                        score++
                        scoreText.text = "Score: $score"
                        passedPipes.add(topPipe)
                    }
                }

                pipes.removeAll { it.x < -64 }
                passedPipes.removeAll { it.x < -64 }

                base1.bringToTop()
                base2.bringToTop()
                scoreText.bringToTop()

                for (pipe in pipes) {
                    if (bird.collidesWith(pipe)) {
                        launch {
                            hitSound.play()
                        }

                        gameState = GameState.GameOver
                        gameOverView.visible = true
                        gameOverView.bringToTop()
                    }
                }
            }
        }
    }
}
