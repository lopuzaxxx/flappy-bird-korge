import korlibs.image.format.*
import korlibs.io.file.std.*
import korlibs.korge.view.*

suspend fun SContainer.setBackground() {
    val backgroundImage = resourcesVfs["background-day.png"].readBitmap()

    image(backgroundImage) {
        anchor(0.0, 0.5)
        position(256, 256)
    }
    image(backgroundImage) {
        anchor(1.0, 0.5)
        position(256, 256)
    }
}
