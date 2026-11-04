package com.santilega.scrollparty

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameTest {
    private val members = listOf("ana", "bob", "eva")

    @Test fun acertarYEngañarPuntuan() {
        val rounds = listOf(
            Round("1", "u", "ana", mapOf("bob" to "ana", "eva" to "bob")),
            Round("2", "u", "bob", mapOf("ana" to "eva", "eva" to "eva")),
        )
        val byId = standings(rounds, members).associateBy { it.memberId }
        assertEquals(Standing("ana", 0, 1), byId["ana"])
        assertEquals(Standing("bob", 1, 2), byId["bob"])
        assertEquals(Standing("eva", 0, 0), byId["eva"])
    }

    @Test fun lasRondasSinRevelarNoCuentan() {
        val rounds = listOf(Round("1", "u", null, mapOf("bob" to "ana")))
        assertEquals(0, standings(rounds, members).sumOf { it.score })
    }

    @Test fun elAutorNoPuntuaConSuPropioVoto() {
        val rounds = listOf(Round("1", "u", "ana", mapOf("ana" to "ana", "bob" to "bob")))
        assertEquals(Standing("ana", 0, 1), standings(rounds, members).first { it.memberId == "ana" })
    }

    @Test fun losEmpatesPremianATodosYElCeroANadie() {
        val table = listOf(Standing("ana", 2, 0), Standing("bob", 2, 0), Standing("eva", 1, 0))
        assertEquals(listOf("ana", "bob"), winners(table) { it.correct })
        assertEquals(emptyList<String>(), winners(table) { it.fooled })
    }

    @Test fun extraeEnlacesDeLasTresPlataformas() {
        assertEquals("https://www.instagram.com/reel/abc/", extractLink("Mira esto https://www.instagram.com/reel/abc/ jaja"))
        assertEquals("https://vm.tiktok.com/xyz", extractLink("https://vm.tiktok.com/xyz"))
        assertEquals("https://youtu.be/abc", extractLink("https://youtu.be/abc"))
        assertEquals("https://youtube.com/shorts/abc", extractLink("https://youtube.com/shorts/abc"))
    }

    @Test fun rechazaLoDemas() {
        assertNull(extractLink("hola"))
        assertNull(extractLink("https://evil.example/instagram.com/reel"))
        assertNull(extractLink("https://youtube.com.evil.example/x"))
        assertNull(extractLink("http://youtu.be/abc"))
    }

    @Test fun elCodigoTiene6CaracteresDictables() {
        repeat(100) { assertEquals(true, newCode().matches(Regex("[A-HJ-NP-Z2-9]{6}"))) }
    }

    @Test fun elColorSiempreEsLibreMientrasQuedenYValidoSiNo() {
        repeat(50) {
            val taken = (0 until 11).toList()
            assertEquals(11, freeColor(taken))
            assertEquals(true, freeColor(listOf(0, 1, 2)) in 3..11)
        }
        assertEquals(true, freeColor((0 until 12).toList()) in 0..11)
    }

    @Test fun laTeleSoloLanzaReaccionesNuevasOCambiadas() {
        val old = mapOf("ana" to "laugh", "bob" to "fire")
        val new = mapOf("ana" to "laugh", "bob" to "skull", "eva" to "sleep")
        assertEquals(listOf("skull", "sleep"), reactionChanges(old, new))
        assertEquals(emptyList<String>(), reactionChanges(new, new))
    }

    @Test fun reelDeLaNocheEsDelAutorConMasRisasYCalaveras() {
        val rounds = listOf(
            Round("1", "u", "ana", emptyMap(), mapOf("bob" to "laugh", "eva" to "fire")),
            Round("2", "u", "bob", emptyMap(), mapOf("ana" to "laugh", "eva" to "skull")),
            Round("3", "u", null, emptyMap(), mapOf("ana" to "laugh", "bob" to "laugh", "eva" to "laugh")), // sin revelar: no cuenta
        )
        assertEquals("bob", reelOfTheNight(rounds))
    }

    @Test fun sinRisasNoHayReelDeLaNoche() {
        assertNull(reelOfTheNight(listOf(Round("1", "u", "ana", emptyMap(), mapOf("bob" to "fire")))))
        assertNull(reelOfTheNight(emptyList()))
    }

    @Test fun inicialesDeUnoODosNombres() {
        assertEquals("A", initials("ana"))
        assertEquals("AL", initials("  ana lópez  garcía "))
        assertEquals("", initials("  "))
    }
}
