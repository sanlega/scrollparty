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
}
