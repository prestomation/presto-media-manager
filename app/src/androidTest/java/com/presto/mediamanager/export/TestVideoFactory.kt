package com.presto.mediamanager.export

import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File

/**
 * Generates a small, valid H.264 MP4 (video only) at runtime so the export
 * instrumentation test has a real, codec-decodable input without bundling a
 * binary asset. Frames are flat YUV with luma varying per frame.
 */
object TestVideoFactory {

    fun createVideoOnlyMp4(
        output: File,
        width: Int = 1280,
        height: Int = 720,
        frameRate: Int = 24,
        frameCount: Int = 36,
    ) {
        val mime = MediaFormat.MIMETYPE_VIDEO_AVC
        val format = MediaFormat.createVideoFormat(mime, width, height).apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible,
            )
            setInteger(MediaFormat.KEY_BIT_RATE, 3_000_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val codec = MediaCodec.createEncoderByType(mime)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()

        val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var trackIndex = -1
        var muxerStarted = false
        val info = MediaCodec.BufferInfo()
        val frameDurationUs = 1_000_000L / frameRate
        var frameIndex = 0
        var inputDone = false

        while (true) {
            if (!inputDone) {
                val inIndex = codec.dequeueInputBuffer(10_000)
                if (inIndex >= 0) {
                    if (frameIndex >= frameCount) {
                        codec.queueInputBuffer(
                            inIndex, 0, 0, frameIndex * frameDurationUs,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                        )
                        inputDone = true
                    } else {
                        val image = codec.getInputImage(inIndex)!!
                        generateFrame(image, frameIndex)
                        codec.queueInputBuffer(
                            inIndex, 0, width * height * 3 / 2,
                            frameIndex * frameDurationUs, 0,
                        )
                        frameIndex++
                    }
                }
            }

            val outIndex = codec.dequeueOutputBuffer(info, 10_000)
            when {
                outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    trackIndex = muxer.addTrack(codec.outputFormat)
                    muxer.start()
                    muxerStarted = true
                }
                outIndex >= 0 -> {
                    val encoded = codec.getOutputBuffer(outIndex)!!
                    if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                    if (info.size > 0 && muxerStarted) {
                        encoded.position(info.offset)
                        encoded.limit(info.offset + info.size)
                        muxer.writeSampleData(trackIndex, encoded, info)
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                }
            }
        }

        codec.stop()
        codec.release()
        muxer.stop()
        muxer.release()
    }

    private fun generateFrame(image: Image, frameIndex: Int) {
        val width = image.width
        val height = image.height
        val luma = (16 + (frameIndex * 8) % 200).toByte()
        fillPlane(image.planes[0], width, height, luma)
        fillPlane(image.planes[1], width / 2, height / 2, 128.toByte())
        fillPlane(image.planes[2], width / 2, height / 2, 128.toByte())
    }

    /** Absolute writes only, so interleaved (semi-planar) chroma planes aren't corrupted. */
    private fun fillPlane(plane: Image.Plane, width: Int, height: Int, value: Byte) {
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        for (y in 0 until height) {
            val base = y * rowStride
            for (x in 0 until width) {
                buffer.put(base + x * pixelStride, value)
            }
        }
    }
}
