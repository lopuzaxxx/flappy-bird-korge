# FlappyBird AI

A Flappy Bird clone built with the [KorGE Game Engine](https://github.com/korlibs/korge) featuring
neural networks that learn to play through neuroevolution.

## Features

- Classic Flappy Bird gameplay with original look and feel
- Neural network AI that evolves to master the game
- Visual representation of the AI's neural networks
- Play yourself or watch the AI learn
- Cross-platform support (Desktop, Web, Mobile)

## About the AI

This project implements neuroevolution of augmenting topologies (NEAT) where neural networks evolve
by:

- Mutating weights, biases, and activation functions
- Adding or removing nodes and connections
- Crossover between high-performing networks

The AI starts with no knowledge of the game and progressively improves through generations of
evolution.

## Controls

- **Space/Click/Tap**: Make the bird flap
- **R**: Restart the game
- **A**: Toggle AI play
- **V**: Toggle neural network visualization

## Build & Run

This project uses Gradle for building:

```bash
# Run on JVM/Desktop
./gradlew runJvm

# Run on Web
./gradlew runJs

# Run on Android
./gradlew runAndroidDebug

# Run on iOS
./gradlew runIosDeviceDebug
```

## Technologies

- [KorGE Game Engine](https://github.com/korlibs/korge) - Multiplatform Kotlin game engine
- Kotlin Multiplatform - For cross-platform code sharing
- Custom Neural Network implementation for AI

## Assets

Game assets from [samuelcust/flappy-bird-assets](https://github.com/samuelcust/flappy-bird-assets).

## License

MIT License
