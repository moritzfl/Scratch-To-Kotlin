package de.moritzf.picoboard

import java.io.IOException

public open class PicoBoardException(
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause)

public class PicoBoardProtocolException(
    message: String,
    cause: Throwable? = null,
) : PicoBoardException(message, cause)

public class PicoBoardPortSelectionException(
    message: String,
    cause: Throwable? = null,
) : PicoBoardException(message, cause)
