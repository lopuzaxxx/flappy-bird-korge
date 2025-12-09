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

class GameScene : BaseFlappyBirdScene() {
    enum class GameState {
        Ready, Running, GameOver
    }

    override suspend fun SContainer.sceneMain() {
        var gameState = GameState.Ready
        setBackground()

        // Initialize shared resources
        initResources()

        // Additional game-specific resources
        val flapSound = resourcesVfs["wing.wav"].readSound()
        val gameOverImage = resourcesVfs["gameover.png"].readBitmap()
        val messageImage = resourcesVfs["message.png"].readBitmap()

        // Initialize ground
        initGround()

        val bird = sprite(bluebirdAnimation) {
            position(birdStartX, birdStartY)
        }

        bird.playAnimationLooped(spriteDisplayTime = 100.milliseconds)

        var velocity = 0.0

        var score = 0
        val scoreText = text("Score: $score") {
            position(10, 10)
        }

        val passedPipes = mutableSetOf<View>()

        val gameOverView = image(gameOverImage) {
            anchor(0.5, 0.5)
            position(256, 256)
            visible = false
        }

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

        // Set up pipe spawner using the common pipe spawner function
        setupPipeSpawner { middle ->
            if (gameState == GameState.Running) {
                createPipePair(512.0, middle)
            }
        }

        addUpdater { dt ->
            if (gameState == GameState.Running) {
                if (isAiPlaying) {
                    val nextPipe = pipes.firstOrNull { it.x + it.width > bird.x }
                    if (nextPipe != null) {
                        val aiGap = 150
                        val pipeCenter = nextPipe.y + aiGap / 2
                        if (bird.y > pipeCenter) {
                            velocity = -250.0
                        }
                    }
                }

                // Update pipes using common function
                updatePipes()

                for (i in pipes.indices step 2) {
                    val topPipe = pipes[i]
                    if (topPipe.x + topPipe.width < bird.x && !passedPipes.contains(topPipe)) {
                        launch { pointSound.play() }
                        score++
                        scoreText.text = "Score: $score"
                        passedPipes.add(topPipe)
                    }
                }

                passedPipes.removeAll { it.x < -64 }

                bringGroundToTop()
                scoreText.bringToTop()

                // Check collision using common function
                if (checkPipeCollision(bird)) {
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
