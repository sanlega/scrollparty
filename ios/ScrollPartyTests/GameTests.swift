import UIKit
import XCTest
@testable import ScrollParty

final class GameTests: XCTestCase {
    private let members = ["ana", "bob", "eva"]

    func testAcertarYEnganarPuntuan() {
        let rounds = [
            Round(id: "1", url: "u", authorId: "ana", votes: ["bob": "ana", "eva": "bob"]),
            Round(id: "2", url: "u", authorId: "bob", votes: ["ana": "eva", "eva": "eva"]),
        ]
        let table = Dictionary(uniqueKeysWithValues: standings(rounds: rounds, memberIds: members).map { ($0.memberId, $0) })
        XCTAssertEqual(table["ana"], Standing(memberId: "ana", correct: 0, fooled: 1))
        XCTAssertEqual(table["bob"], Standing(memberId: "bob", correct: 1, fooled: 2))
        XCTAssertEqual(table["eva"], Standing(memberId: "eva", correct: 0, fooled: 0))
    }

    func testLasRondasSinRevelarNoCuentan() {
        let rounds = [Round(id: "1", url: "u", authorId: nil, votes: ["bob": "ana"])]
        XCTAssertEqual(standings(rounds: rounds, memberIds: members).map(\.score).reduce(0, +), 0)
    }

    func testElAutorNoPuntuaConSuPropioVoto() {
        let rounds = [Round(id: "1", url: "u", authorId: "ana", votes: ["ana": "ana", "bob": "bob"])]
        XCTAssertEqual(
            standings(rounds: rounds, memberIds: members).first { $0.memberId == "ana" },
            Standing(memberId: "ana", correct: 0, fooled: 1)
        )
    }

    func testLaClasificacionVaDeMayorAMenorYMantieneElOrdenEnEmpates() {
        let rounds = [Round(id: "1", url: "u", authorId: "eva", votes: ["ana": "bob", "bob": "ana"])]
        XCTAssertEqual(standings(rounds: rounds, memberIds: members).map(\.memberId), ["eva", "ana", "bob"])
    }

    func testLosEmpatesPremianATodosYElCeroANadie() {
        let table = [
            Standing(memberId: "ana", correct: 2, fooled: 0),
            Standing(memberId: "bob", correct: 2, fooled: 0),
            Standing(memberId: "eva", correct: 1, fooled: 0),
        ]
        XCTAssertEqual(winners(table) { $0.correct }, ["ana", "bob"])
        XCTAssertEqual(winners(table) { $0.fooled }, [])
    }

    func testExtraeEnlacesDeLasTresPlataformas() {
        XCTAssertEqual(extractLink("Mira esto https://www.instagram.com/reel/abc/ jaja"), "https://www.instagram.com/reel/abc/")
        XCTAssertEqual(extractLink("https://vm.tiktok.com/xyz"), "https://vm.tiktok.com/xyz")
        XCTAssertEqual(extractLink("https://youtu.be/abc"), "https://youtu.be/abc")
        XCTAssertEqual(extractLink("https://youtube.com/shorts/abc"), "https://youtube.com/shorts/abc")
    }

    func testRechazaLoDemas() {
        XCTAssertNil(extractLink("hola"))
        XCTAssertNil(extractLink("https://evil.example/instagram.com/reel"))
        XCTAssertNil(extractLink("https://youtube.com.evil.example/x"))
        XCTAssertNil(extractLink("http://youtu.be/abc"))
    }

    func testElCodigoTiene6CaracteresDictables() {
        for _ in 0..<100 {
            XCTAssertNotNil(newCode().wholeMatch(of: /[A-HJ-NP-Z2-9]{6}/))
        }
    }

    // MARK: - Premios

    func testReelDeLaNocheEsElAutorConMasRisasYCalaveras() {
        let rounds = [
            Round(id: "1", url: "u", authorId: "ana", votes: [:], reactions: ["bob": "laugh", "eva": "fire"]),
            Round(id: "2", url: "u", authorId: "bob", votes: [:], reactions: ["ana": "laugh", "eva": "skull", "zoe": "sleep"]),
            Round(id: "3", url: "u", authorId: "eva", votes: [:], reactions: ["ana": "fire", "bob": "fire"]),
        ]
        XCTAssertEqual(reelOfTheNight(rounds: rounds), "bob")
    }

    func testReelDeLaNocheEnEmpateGanaLaPrimeraYSinReaccionesNadie() {
        let tied = [
            Round(id: "1", url: "u", authorId: "ana", votes: [:], reactions: ["bob": "laugh"]),
            Round(id: "2", url: "u", authorId: "bob", votes: [:], reactions: ["ana": "skull"]),
        ]
        XCTAssertEqual(reelOfTheNight(rounds: tied), "ana")
        let none = [Round(id: "1", url: "u", authorId: "ana", votes: [:], reactions: ["bob": "fire", "eva": "sleep"])]
        XCTAssertNil(reelOfTheNight(rounds: none))
        XCTAssertNil(reelOfTheNight(rounds: [Round(id: "1", url: "u", authorId: nil, votes: [:], reactions: ["bob": "laugh"])]))
    }

    func testTopDetectiveYMasterBluffer() {
        let rounds = [
            Round(id: "1", url: "u", authorId: "ana", votes: ["bob": "ana", "eva": "bob"]),
            Round(id: "2", url: "u", authorId: "ana", votes: ["bob": "ana", "eva": "ana"]),
        ]
        let table = standings(rounds: rounds, memberIds: members)
        XCTAssertEqual(winners(table) { $0.correct }, ["bob"])
        XCTAssertEqual(winners(table) { $0.fooled }, ["ana"])
    }

    // MARK: - Color

    func testElColorPreferidoSeRespetaSiEstaLibre() {
        let me = Member(id: "z", name: "Zoe", joinedAt: 3)
        let others = [Member(id: "a", name: "A", color: 0, joinedAt: 1), Member(id: "b", name: "B", color: 1, joinedAt: 2)]
        XCTAssertEqual(assignColor(me: me, preferred: 7, members: others + [me]), 7)
    }

    func testSiElColorEstaOcupadoSeDaElPrimeroLibre() {
        let me = Member(id: "z", name: "Zoe", color: 0, joinedAt: 3)
        let others = [Member(id: "a", name: "A", color: 0, joinedAt: 1), Member(id: "b", name: "B", color: 1, joinedAt: 2)]
        XCTAssertEqual(assignColor(me: me, preferred: 0, members: others + [me]), 2)
    }

    func testElQueEntroAntesConservaSuColor() {
        let early = Member(id: "a", name: "A", color: 4, joinedAt: 1)
        let late = Member(id: "b", name: "B", color: 4, joinedAt: 2)
        XCTAssertEqual(assignColor(me: early, preferred: 4, members: [early, late]), 4)
        XCTAssertNotEqual(assignColor(me: late, preferred: 4, members: [early, late]), 4)
    }

    func testConLos12OcupadosNoSeSaleDelRango() {
        let all = (0..<12).map { Member(id: "m\($0)", name: "M", color: $0, joinedAt: Int64($0)) }
        let me = Member(id: "z", name: "Zoe", color: 3, joinedAt: 99)
        XCTAssertTrue((0..<12).contains(assignColor(me: me, preferred: 3, members: all + [me])))
    }

    // MARK: - Enlaces

    func testExtractLinkConTextoAlrededorYLimites() {
        XCTAssertEqual(extractLink("jajaja https://www.tiktok.com/@a/video/1?x=1 mira"), "https://www.tiktok.com/@a/video/1?x=1")
        XCTAssertNil(extractLink("https://www.instagram.com/" + String(repeating: "a", count: 500)))
        XCTAssertNil(extractLink("HTTPS://WWW.INSTAGRAM.COM/reel/abc"))
        XCTAssertNil(extractLink("https://instagram.com"))
    }

    // MARK: - Contraste

    func testLosParesDeTextoDelSistemaCumplen45() {
        for pair in Hex.textPairs {
            XCTAssertGreaterThanOrEqual(contrastRatio(pair.text, pair.background), 4.5, pair.name)
        }
    }

    func testLasInicialesEnInkSeLeenSobreLos12ColoresDeJugador() {
        XCTAssertEqual(Hex.player.count, playerColorCount)
        for color in Hex.player {
            XCTAssertGreaterThanOrEqual(contrastRatio(Hex.ink, color), 4.5, String(color, radix: 16))
        }
    }

    func testLosControlesCumplen3() {
        XCTAssertGreaterThanOrEqual(contrastRatio(Hex.coral, Hex.white), 3)
        XCTAssertGreaterThanOrEqual(contrastRatio(Hex.yellow, Hex.blue), 3)
        XCTAssertGreaterThanOrEqual(contrastRatio(Hex.white, Hex.night2), 3)
    }

    // MARK: - Selfie

    func testLaFotoQuedaCuadradaDe256YCabeEnElLimite() throws {
        let size = CGSize(width: 1200, height: 800)
        let image = UIGraphicsImageRenderer(size: size).image { context in
            for _ in 0..<4000 {
                UIColor(red: .random(in: 0...1), green: .random(in: 0...1), blue: .random(in: 0...1), alpha: 1).setFill()
                context.fill(CGRect(x: .random(in: 0...1200), y: .random(in: 0...800), width: 30, height: 30))
            }
        }
        let encoded = try XCTUnwrap(encodePhoto(image))
        XCTAssertLessThanOrEqual(encoded.count, maxPhotoChars)
        let data = try XCTUnwrap(Data(base64Encoded: encoded))
        let decoded = try XCTUnwrap(UIImage(data: data))
        XCTAssertEqual(decoded.size.width * decoded.scale, 256)
        XCTAssertEqual(decoded.size.height * decoded.scale, 256)
    }

    func testLasInicialesSonDosComoMucho() {
        XCTAssertEqual(initials("ana maría lópez"), "AM")
        XCTAssertEqual(initials("Leo"), "L")
        XCTAssertEqual(initials(""), "")
    }
}
