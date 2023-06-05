import java.util.*

// Extensions
fun <T> List<T>.addAllIf(condition: Boolean, elements: List<T>): List<T> {
    return if (condition) this.plus(elements) else this
}

fun <T> List<T>.addIf(condition: Boolean, element: T): List<T> {
    return if (condition) this.plus(element) else this
}

// Objects
object Game {
    val cells = mutableListOf<Cell>()
    val bases = mutableListOf<Base>()
}

class Base {
    enum class Type {
        MINE, ENEMY
    }

    var type = Type.MINE
    var index = 0

    fun mapToCell() = Game.cells.first { it.index == index }
}

class Cell {
    enum class Type {
        VOID, EMPTY, EGG, CRYSTAL
    }

    var index = 0
    var type = Type.EMPTY
    var resourcesAmount = 0
    var neighbors = mutableListOf<Cell>()
    var ants = mutableListOf<Ant>()
}

class Ant {
    enum class Type {
        MINE, ENEMY
    }

    var type = Type.MINE
    var cell = Cell()
}

// Actions
object Action {

    fun startWith(vararg actions: String) {
        println(actions.joinToString(";"))
    }

    fun skip() = "WAIT"

    fun line(source: Cell, target: Cell, strength: Int): String {
        return "LINE ${source.index} ${target.index} $strength"
    }

    fun beacon(cell: Cell, strength: Int): String {
        return "BEACON ${cell.index} $strength"
    }

    fun message(text: String) = "MESSAGE $text"
}

// Utils
object Utils {

    fun shortestDistance(source: Cell, target: Cell): Int {
        return shortestDistanceRecursive(source, target, mutableSetOf(), listOf())
    }

    private fun shortestDistanceRecursive(currentCell: Cell, target: Cell, visited: MutableSet<Cell>, currentPath: List<Cell>): Int {
        // Si la cellule courante est la cellule cible, retourner la distance de 0
        if (currentCell == target) {
            return 0
        }

        // Marquer la cellule courante comme visitée
        visited.add(currentCell)

        // Parcourir les cellules voisines
        var shortestDistance = Int.MAX_VALUE
        for (neighbor in currentCell.neighbors) {
            // Ignorer les cellules de type VOID ou déjà visitées
            if (neighbor.type == Cell.Type.VOID || neighbor in visited) {
                continue
            }

            // Appel récursif pour trouver la distance la plus courte entre la cellule voisine et la cellule cible
            val distance = shortestDistanceRecursive(neighbor, target, visited, currentPath + neighbor)

            // Mettre à jour la distance la plus courte si nécessaire
            if (distance != -1 && distance < shortestDistance) {
                shortestDistance = distance
            }
        }

        // Si une distance valide a été trouvée, retourner la distance augmentée de 1
        if (shortestDistance != Int.MAX_VALUE) {
            return shortestDistance + 1
        }

        // Aucun chemin trouvé, retourner une valeur indiquant l'absence de chemin
        return -1
    }
}

// Main
fun main() {
    val input = Scanner(System.`in`)
    val numberOfCells = input.nextInt() // amount of hexagonal cells in this map
    for (i in 0 until numberOfCells) {
        val typeNumber = input.nextInt() // 0 for empty, 1 for eggs, 2 for crystal
        val initialResources = input.nextInt() // the initial amount of eggs/crystals on this cell
        // val neigh0 = input.nextInt() // the index of the neighbouring cell for each direction

        Game.cells.add(Cell().apply {
            index = i
            type = when (typeNumber) {
                -1 -> Cell.Type.VOID
                0 -> Cell.Type.EMPTY
                1 -> Cell.Type.EGG
                2 -> Cell.Type.CRYSTAL
                else -> Cell.Type.EMPTY
            }
            resourcesAmount = initialResources
            neighbors.addAll((0 until 6).map {
                Cell().apply {
                    index = input.nextInt()
                }
            })
        })
    }
    val numberOfBases = input.nextInt()
    for (i in 0 until numberOfBases) {
        val myBaseIndex = input.nextInt()
        Game.bases.add(Base().apply {
            index = myBaseIndex
        })
    }
    for (i in 0 until numberOfBases) {
        val oppBaseIndex = input.nextInt()
        Game.bases.add(Base().apply {
            type = Base.Type.ENEMY
            index = oppBaseIndex
        })
    }

    // game loop
    while (true) {
        for (i in 0 until numberOfCells) {
            val resources = input.nextInt() // the current amount of eggs/crystals on this cell
            val myAnts = input.nextInt() // the amount of your ants on this cell
            val oppAnts = input.nextInt() // the amount of opponent ants on this cell

            Game.cells[i].apply cell@ {
                if (resourcesAmount > 0 && resources == 0) {
                    type = when (type) {
                        Cell.Type.EGG -> Cell.Type.EMPTY
                        Cell.Type.CRYSTAL -> Cell.Type.EMPTY
                        else -> type
                    }
                }
                resourcesAmount = resources
                ants.clear()
                ants.addAll((0 until myAnts).map {
                    Ant().apply {
                        cell = this@cell
                    }
                })
                ants.addAll((0 until oppAnts).map {
                    Ant().apply {
                        type = Ant.Type.ENEMY
                        cell = this@cell
                    }
                })
            }
        }

        // GAME LOGIC
        val availableResources = Game.cells.filter { it.type == Cell.Type.CRYSTAL }
        val availableEggs = Game.cells.filter { it.type == Cell.Type.EGG }
        val myBasesCells = Game.bases.filter { it.type == Base.Type.MINE }.map { it.mapToCell() }

        // target eggs if there are eggs and we have less ants than the opponent
        val myTotalAnts = Game.cells.sumOf { cell -> cell.ants.size }
        val enemyTotalAnts = Game.cells.sumOf { cell -> cell.ants.count { it.type == Ant.Type.ENEMY } }
        val targetEggs = availableEggs.isNotEmpty() && myTotalAnts < enemyTotalAnts

        // get the closest distances between my bases and available resources. return a list of ((base, resource), distance)
        val distances = availableResources.addAllIf(targetEggs, availableEggs).map { base ->
            myBasesCells.map { resource ->
                Triple(base, resource, Utils.shortestDistance(base, resource))
            }
        }.flatten().sortedBy { it.third }

        // target 2 crystals and 1 egg if we target eggs, or 3 crystals if we don't
        val targetResources = distances
            .filter { distance -> distance.second.type == Cell.Type.CRYSTAL }.take(3 - if (targetEggs) 1 else 0)
            .addIf(targetEggs, distances.first { distance -> distance.second.type == Cell.Type.EGG })

        Action.startWith(
            *targetResources.map { (base, resource, _) ->
                val strength = if (resource.type == Cell.Type.EGG) 1 else 2
                Action.line(base, resource, strength)
            }.toTypedArray()
        )
    }
}
