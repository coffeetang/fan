import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.cos
import kotlin.math.sin

const val STATE_CLOSE = "closed"
const val STATE_ON = "turnOn"

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun fans() {
    var circleX by remember { mutableStateOf(600f) }
    var circleY by remember { mutableStateOf(600f) }
    val circleRdius = circleX.coerceAtMost(circleY) - 200.dp.value
    val colorList = listOf(
        listOf(Color(0xff757575), Color(0xfff9f9f9)),
        listOf(Color.Transparent, Color.Transparent),
        listOf(Color(0xff757575), Color(0xfff9f9f9)),
        listOf(Color.Transparent, Color.Transparent),
        listOf(Color(0xff757575), Color(0xfff9f9f9)),
        listOf(Color.Transparent, Color.Transparent),
    )
    val angleList = Array(6) { 0f + it * 60f }
    val lineAngleList = Array(45) { 0f + it * 8f }
    val framePath = Path().apply {
        moveTo(circleX, circleY)
        addOval(
            Rect(
                circleX - circleRdius - 100f, circleY - circleRdius - 100f,
                circleX + circleRdius + 100f, circleY + circleRdius + 100f
            )
        )
        close()
    }
    val switcher = remember { mutableStateOf(STATE_CLOSE) }
    val fanState = remember { mutableStateOf(1000) }

    //-------------------------------------------

    val transition = rememberInfiniteTransition()
    val wheelState = transition.animateFloat(
        fanState.value.toFloat(), fanState.value+360f,
        animationSpec = InfiniteRepeatableSpec(
            tween(fanState.value, easing = LinearEasing)
        )
    )


    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier.fillMaxWidth().height(500.dp)
        ) {
            circleX = size.center.x
            circleY = size.center.y
            drawLine(
                color = Color(0xffE6A639),
                start = Offset(circleX, circleY),
                end = Offset(circleX, circleY + circleRdius * 2),
                strokeWidth = 35f,
                cap = StrokeCap.Round
            )

            drawPath(framePath, color = Color(0xff212121), style = Stroke(30f))

            for (index in colorList.indices) {
                drawArc(
                    brush = Brush.horizontalGradient(colorList[index], tileMode = TileMode.Mirror),
                    startAngle = if(switcher.value == STATE_ON) wheelState.value+angleList[index] else angleList[index],
                    sweepAngle = 60f,
                    useCenter = true,
                    size = Size(circleRdius * 2, circleRdius * 2),
                    topLeft = Offset(circleX - circleRdius, circleY - circleRdius),
                )
            }


            for (angleIndex in lineAngleList.indices) {
                drawLine(
                    color = Color(0xff212121),
                    start = Offset(circleX, circleY),
                    end = Offset(
                        pointX(circleRdius + 100.dp.value, circleX, lineAngleList[angleIndex]),
                        pointY(circleRdius + 100.dp.value, circleY, lineAngleList[angleIndex])
                    ),
                    strokeWidth = 10f,
                    cap = StrokeCap.Round
                )
            }
        }
        Row(
            modifier = Modifier.size(250.dp, 60.dp).clip(RoundedCornerShape(10.dp))
                .background(Color(0xffE6A639)),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.padding(10.dp).fillMaxHeight().fillMaxWidth(0.5f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.padding(5.dp).size(20.dp).clip(RoundedCornerShape(10.dp))
                    .background(if (switcher.value != STATE_CLOSE) Color(0xffFF4252) else Color(0xffcccccc))
                    .pointerInput(Unit) {
                        detectTapGestures {
                            fanState.value = 1000
                        }
                    })
                Spacer(modifier = Modifier.padding(5.dp).size(20.dp).clip(RoundedCornerShape(10.dp))
                    .background(
                        if (switcher.value != STATE_CLOSE && fanState.value < 1000)
                            Color(0xffFF4252)
                        else
                            Color(0xffcccccc)
                    ).pointerInput(Unit) {
                        detectTapGestures {
                            fanState.value = 600
                        }
                    })
                Spacer(modifier = Modifier.padding(5.dp).size(20.dp).clip(RoundedCornerShape(10.dp))
                    .background(
                        if (switcher.value != STATE_CLOSE && fanState.value < 600)
                            Color(0xffFF4252)
                        else
                            Color(0xffcccccc)
                    ).pointerInput(Unit) {
                        detectTapGestures {
                            fanState.value = 200
                        }
                    })
            }

            val blockSize = 24.dp
            val blockSizePx = with(LocalDensity.current) {
                blockSize.toPx()
            }
            val swipeState = rememberSwipeableState(initialValue = "start")
            val mAnchor = mapOf(0f to "start", blockSizePx to "end")
            if (swipeState.currentValue == "start") {
                switcher.value = STATE_CLOSE
            } else {
                switcher.value = STATE_ON
            }
            val flag = remember { mutableStateOf(false) }
            LaunchedEffect(flag.value) {
                if (flag.value) {
                    swipeState.animateTo("end")
                } else {
                    swipeState.animateTo("start")
                }
            }
            Box(
                modifier = Modifier.padding(end = 10.dp).size(blockSize * 2, blockSize)
                    .clip(RoundedCornerShape(12.dp)).background(Color(0x80cccccc))
                    .pointerInput(Unit){
                        detectTapGestures {
                            flag.value = !flag.value
                        }
                    }
            ) {
                Box(modifier = Modifier
                    .offset { IntOffset(swipeState.offset.value.toInt(), 0) }
                    .swipeable(
                        state = swipeState, anchors = mAnchor,
                        thresholds = { from, _ ->
                            if (from == "start") {
                                FractionalThreshold(0.3f)
                            } else {
                                FractionalThreshold(0.5f)
                            }
                        }, orientation = Orientation.Horizontal
                    )
                    .size(blockSize)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color = if (switcher.value == STATE_ON) Color(0xffFF4252) else Color.Gray)
                    .padding(8.dp)
                )
            }
        }

    }
}


private fun pointX(width: Float, centerX: Float, fl: Float): Float {
    val angle = Math.toRadians(fl.toDouble())
    return centerX - cos(angle).toFloat() * (width)
}

private fun pointY(width: Float, centerY: Float, fl: Float): Float {
    val angle = Math.toRadians(fl.toDouble())
    return centerY - sin(angle).toFloat() * (width)
}