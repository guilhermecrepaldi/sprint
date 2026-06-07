package com.strava_matematica.domain.procedural

import java.util.Calendar

/**
 * Gerenciador modular da Arena Competitiva e Simulados Ranqueados.
 * Permite que a semente fixa (seed) seja alterada via backend no futuro,
 * atrelada a Campeonatos, Ligas ou Datas Específicas.
 */
object ArenaManager {
    // Pode ser atualizado no futuro consumindo uma API do backend
    var activeTournamentId: String = "S1-LIGA-DIARIA"
    var overrideSeed: Long? = null

    /**
     * Retorna a semente determinística do campeonato atual.
     * Atualmente baseia-se na data (para gerar o mesmo simulado diário para todos)
     * ou usa um ID de campeonato estático.
     */
    fun getCurrentTournamentSeed(): Long {
        if (overrideSeed != null) return overrideSeed!!
        
        // Exemplo modular: O campeonato diário muda a semente todos os dias
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        
        // Seed diária (Ex: 2026155) garante a mesma prova nas 24h para o mesmo MMR/Config
        val dailySeed = (year.toLong() * 1000) + dayOfYear.toLong()
        return dailySeed
    }
}
