import korlibs.korge.scene.*

actual fun provideScene(): Scene {
    // For WASM, you can return an empty scene or a simplified version of your game
    return GameScene()
}
