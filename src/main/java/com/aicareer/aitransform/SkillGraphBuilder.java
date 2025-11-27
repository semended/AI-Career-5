package com.aicareer.aitransform;

import java.util.*;

public class SkillGraphBuilder {

    /**
     * @param skillOrder   фиксированный список навыков (столбцы)
     * @param rows         список строк матрицы; каждая строка = VacancySkills (skills: Map<String, Integer>)
     * @param minCoOccur   минимальное число совместных появлений, чтобы вообще рассматривать пару
     * @param minProb      минимальное P(B|A), чтобы добавить ребро A -> B
     */
    public static SkillGraph buildDirectedGraph(
            List<String> skillOrder,
            List<VacancySkills> rows,
            int minCoOccur,
            double minProb
    ) {
        int n = skillOrder.size();
        Map<String, Integer> indexBySkill = new HashMap<>();
        for (int i = 0; i < n; i++) {
            indexBySkill.put(skillOrder.get(i), i);
        }

        // co-occurrence matrix
        int[][] coOccur = new int[n][n];

        // Заполняем coOccur по строкам
        for (VacancySkills vacancySkills : rows) {
            Map<String, Integer> skillsMap = vacancySkills.getSkills();
            boolean[] present = new boolean[n];

            // отмечаем, какие навыки присутствуют в строке
            for (int i = 0; i < n; i++) {
                String skill = skillOrder.get(i);
                int val = skillsMap.getOrDefault(skill, 0);
                if (val > 0) {
                    present[i] = true;
                }
            }

            // увеличиваем счётчики совместных появлений
            for (int i = 0; i < n; i++) {
                if (!present[i]) continue;
                for (int j = 0; j < n; j++) {
                    if (!present[j]) continue;
                    coOccur[i][j]++;
                }
            }
        }

        // Подсчёт количества строк, где встречается каждый навык
        int[] count = new int[n];
        for (int i = 0; i < n; i++) {
            count[i] = coOccur[i][i];
        }

        List<SkillGraph.Edge> edges = new ArrayList<>();

        // Строим граф
        for (int i = 0; i < n; i++) {
            if (count[i] == 0) continue;
            for (int j = i + 1; j < n; j++) {
                if (count[j] == 0) continue;

                int co = coOccur[i][j];
                if (co < minCoOccur) continue; // фильтрация шума

                double pJgivenI = co / (double) count[i];
                double pIgivenJ = co / (double) count[j];

                if (pJgivenI < minProb && pIgivenJ < minProb) {
                    continue;
                }

                String si = skillOrder.get(i);
                String sj = skillOrder.get(j);

                // задаем направление ребру (если навык встречается чаще, то стрелка идёт от него)
                if (count[i] > count[j] && pJgivenI > pIgivenJ && pJgivenI >= minProb) {
                    edges.add(new SkillGraph.Edge(si, sj, pJgivenI)); // si -> sj
                } else if (count[j] > count[i] && pIgivenJ > pJgivenI && pIgivenJ >= minProb) {
                    edges.add(new SkillGraph.Edge(sj, si, pIgivenJ)); // sj -> si
                }
            }
        }

        return new SkillGraph(new HashSet<>(skillOrder), edges);
    }
}
