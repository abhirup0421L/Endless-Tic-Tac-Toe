package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.ai.EndlessAiEngine
import com.example.domain.model.AiDifficulty
import com.example.domain.model.BoardPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Endless TTT", appName)
  }

  @Test
  fun `test endless 3 piece vanishing mechanic`() {
    // 3 pieces initially
    val initialPieces = listOf(
      BoardPosition(0, 0),
      BoardPosition(0, 1),
      BoardPosition(0, 2)
    )

    // Move to (1, 0)
    val afterMove = EndlessAiEngine.simulateMove(initialPieces, BoardPosition(1, 0))

    // Size must remain 3
    assertEquals(3, afterMove.size)
    // First piece (0, 0) vanished
    assertTrue(BoardPosition(0, 0) !in afterMove)
    // New piece (1, 0) is added as newest
    assertEquals(BoardPosition(1, 0), afterMove.last())
  }

  @Test
  fun `test ai finds winning move`() {
    val aiPieces = listOf(BoardPosition(0, 0), BoardPosition(0, 1))
    val humanPieces = listOf(BoardPosition(1, 0), BoardPosition(1, 1))

    val bestMove = EndlessAiEngine.getBestMove(
      difficulty = AiDifficulty.HARD,
      aiPieces = aiPieces,
      humanPieces = humanPieces
    )

    assertEquals(BoardPosition(0, 2), bestMove)
  }
}
