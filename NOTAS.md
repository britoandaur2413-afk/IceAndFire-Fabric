# Notas del port (Ice and Fire, Fabric 26.3)

## Encontrar los lugares del mod: `/iceandfire find <lugar>`

Para quien puede usar trucos. Dice el más cercano, a cuántos bloques está y da un clic para ir:

- `nest` nido de dragón · `cave` cueva de dragones · `hall` salón del dragón · `kingdom` reino pillager
- `sanctuary` santuario de hadas · `cavern` cueva de hadas · `witches` cabaña de brujas · `hydra` cueva de hidra
- `spire` aguja del terror · `throne` ciudadela de Cryona, la Reina del Terror
- `mausoleum`, `graveyard`, `gorgon_temple` (las del mod original)

También sirve `/locate structure iceandfire_fabric:<nombre>` (dragon_roost, dragon_cave, dragon_hall, pillager_kingdom,
pixie_sanctuary, pixie_cavern, witch_cottage, hydra_cave, dread_spire, dread_throne).

## Estructuras del mod original: difíciles de encontrar (para mejorar más adelante)

Cuesta mucho encontrar las estructuras del mod sin ir a creativo. La idea es mejorarlo más adelante, o usarlo a favor para
dar más dinamismo que el de un mod de fantasía simple. Por ahora no se cambió nada.

- Las tres comparten un mismo reparto (`data/iceandfire_fabric/worldgen/structure_set/structures.json`): a lo más una cada
  32 chunks (512 bloques), con al menos 16 chunks entre ellas. Luego se elige una al azar: cementerio 48, mausoleo 30,
  templo de la gorgona 16. Cada una solo sale en sus biomas (cementerio: tierra; mausoleo: nevados; templo: playas).
- Para hacerlas más frecuentes bastaría con bajar `spacing` y `separation`, o darle a cada una su propio reparto.

## Cobre

- El mod usa el cobre del juego: mena, lingote, pepita, bloque, herramientas y armadura de cobre son los de vanilla (el
  mod ya no tiene los suyos). Lo que un mundo viejo tenía del cobre del mod se lee como el del juego (no se pierde).
- Siguen siendo del mod: los montones de monedas de cobre, los dragones de cobre (sus huevos y escamas) y la armadura
  de dragón de cobre (se hace con bloques de cobre del juego). Los tesoros de los dragones de rayo usan bloques de
  cobre encerados (no se oxidan).

## Estructuras que no se pisan

- Las estructuras del mod se hacen al final (despejan árboles y tierra), así que podían partir por la mitad una aldea,
  una cámara de pruebas o un templo. Ahora una estructura del juego o de otro mod cede su lugar (no se hace) si tocaría
  una del mod; entre las del mod, la más común cede ante la más rara. Las del mod salen igual de seguido, y `/locate`
  y los mapas de exploración nunca llevan a una que cedió. (Las minas abandonadas, los tesoros enterrados y las
  fortalezas del End no ceden.)
- Donde una estructura del mod recorta el terreno ya no queda nada en el aire: pasto, flores, arbustos, nieve,
  faroles o antorchas sin apoyo se quitan (los árboles ya se quitaban). En castillos, ciudadela y reinos (muros y
  torres incluidos) no queda nada de un árbol cortado aunque se apoye en un muro (antes quedaban copas de hojas sobre
  los muros y en el patio).
- En la arena de los salones del dragón las monedas ya no tapan el pedestal de los braseros (las fogatas flotaban).
- Cuevas de hadas: cerca de su boca el túnel queda abierto al cielo (su techo estaría sobre la tierra); ahí ya no
  cuelgan bayas, líquenes, flores de esporas ni faroles con cadena (colgaban de la nada). Lo que cuelga sin apoyo se
  quita de arriba abajo en todas las estructuras del mod.

## Cómo se domestica, eclosiona y monta (probado en los tests)

- Hada: tira (Q) una tarta cerca; la recoge y queda domesticada.
- Hipogrifo: patas de conejo con click derecho, una por una, hasta que acepte.
- Hipocampo: algas (kelp) con click derecho.
- Anfíptero: móntalo con la mano vacía y aguanta en su lomo unos 20 s (muerde un poco).
- Cocatriz: sostenle la mirada a 4 bloques o más hasta que se rinda (unos 50 s; hace daño, lleva regeneración).
- Huevos de dragón: el de fuego sobre fuego; el de hielo en agua (se congela y eclosiona); el de rayo al aire libre
  bajo la lluvia (en desiertos no llueve).
- Dragón: click derecho con la mano vacía para subir (desde la fase 3). Espacio despega, shift en el aire baja, shift
  en tierra te bajas. R aliento, G mordisco, X bajar, F7 vista.
- Horno de dragón: mirando su apertura desde el dragón, R manda el aliento al horno.
- Cuerpo de los dragones: la cabeza, el cuello, las alas (con sus puntas) y la cola (con su aleta) tienen su propia
  caja (se ven con F3+B, como las cabezas de la hidra y el ojo del cíclope) y un golpe en cualquiera de ellas le llega
  al dragón (antes el juego del jugador no sabía de quién era la parte y el golpe se perdía). Las cajas siguen su pose,
  medida en el modelo: en el suelo las alas plegadas a sus costados; volando o planeando las alas abiertas hasta sus
  puntas (tres veces y media su tamaño) y, al planear, la cola colgando. Montado, tus golpes no le dan a tu dragón.
- Arco de hueso de dragón: dispara cualquier flecha (antes solo las de hueso de dragón: con flechas normales ni se
  tensaba); las de hueso de dragón siguen pegando más.
- Empujones: los golpes que lanzan o empujan (el rugido, la embestida y el picado de Black Frost, los de Cryona, el
  anfíptero, el hipogrifo, la reina myrmex, el macuahuitl, los hechizos de las hadas...) ahora sí mueven al jugador
  golpeado (antes solo lo veían los demás). Black Frost empuja fuera a quien se le pega (es un muro).

## Dónde viven los dragones

- Nidos (estructura, uno cada ~24 chunks en zonas altas): un cuenco quemado (congelado o agrietado) en la cima, con
  borde de rocas y huesos, un montón de oro (plata, cobre) y cofres; un dragón de fase 2 o 3 y a veces una cría.
- Nidos: los árboles y setas gigantes que el cuenco corta ya no quedan flotando (al hacerse, cada estructura de
  superficie borra los trozos de árbol que quedaron sin tronco ni apoyo; también reinos, castillos y el terror).
- Cuevas (estructura, bajo tierra con un túnel ancho hasta la superficie): roca del dragón, minerales en las paredes,
  tesoro con cofres, 2 o 3 dragones de fase 4; a veces el tercero es un anciano de fase 5. No entra lava. Ahora
  mucho más grandes (unos 60 bloques de ancho y 17 de alto, túnel de 12 de ancho).
- Salón del dragón (raro, uno cada 72 chunks): castillo en ruinas con la piedra de su tierra (piedra, hielo y abeto
  en el frío, arenisca en el calor). En la torre del homenaje una escalera grande baja a la forja (canales de lava,
  hielo o agua según el dragón, pilares, forjas y salas laterales: almacén, biblioteca, prisión o cuartel). Un puente
  baja sobre un abismo hasta la arena: muros, galerías con braseros, y al fondo, sobre una tarima, el tesoro con
  cofres y el dragón de fase 5 dormido encima. Abismo y arena agrandados: el abismo mide 48 de ancho y 49 de largo,
  la arena 92 x 88 con una bóveda de 24 a 42 bloques de alto, para que el dragón vuele dentro.
- Dragones del cielo: de vez en cuando un dragón joven cruza el cielo (unos 12 minutos por jugador). No ataca si no lo
  atacan. Si matas 2 en 20 minutos, aparece el hada oráculo y luego una oleada de 3 dragones de fase 4.
- Bajo techo bajo (cuevas pequeñas) los dragones no despegan: pelean a pie, trepan con un aleteo (hasta 6 bloques) y
  el jugador que los atacó es su objetivo durante un minuto. En salas tan altas como la arena sí vuelan, siempre
  hacia sitios libres dentro de la sala. Si caminando no avanzan (un muro, una esquina) saltan por encima o retroceden.
- Ataques especiales de los dragones viejos (fase 4 y 5, salvajes), cada 11-18 s en una pelea, siempre avisados en
  el suelo: el de fuego hace llover rocas ardientes en círculos, el de hielo hace brotar picos de un círculo de
  escarcha (y congela), el de rayo lanza rayos en círculos.
- Vuelo: solo despegan con espacio libre para todo el cuerpo (sin ramas ni salientes encima); si en el aire no
  avanzan (chocan con un cerro o un árbol) buscan otro camino y, si siguen trabados, aterrizan.
- Los dragones salvajes no se pelean entre ellos (ni sus alientos se dañan); los de un reino tampoco entre sí.
- El cuerpo del dragón tiene partes en el mundo (cabeza, cuello, alas, cola): se le pega en cualquiera, pero solo
  reciben golpes de jugadores y criaturas (ni paredes, ni asfixia, ni empujones).
- Un dragón muerto no es enemigo: su cuerpo se queda para cosecharlo, pero ninguna hada, mascota u otro mob lo ataca.

## Reinos pillager

- Raros (uno cada 56 chunks): muralla con torres y portón con estandartes, camino con faroles, la sede (gran salón
  con el trono, el evocador y cofres), cuartel con camas, herrería, tiendas, almacén, dianas y alayes enjaulados.
  Hecho con la piedra y madera de su tierra (arenisca en desierto, piedra y abeto en el frío, roble oscuro en otros).
- 2 o 3 dragones jóvenes (fase 3, la primera montable; no crecen) con armadura de hierro y un pillager en el lomo,
  sentados en sus nidos de heno. Se levantan con un rugido cuando llega un enemigo (jugador o golem) y vuelven a
  sentarse cuando ya no hay nadie. Vuelan con su jinete para pelear.
- El jinete se ve sentado en el lomo y tiene su propia hitbox y vida (pillager 24, señor de la guerra 80, caballero
  del terror 40). Él dispara con la ballesta (el señor de la guerra tres virotes y la escarcha); el dragón muerde y
  solo a veces echa aliento. Si matas al jinete cae muerto (con su botín), el dragón baja a tierra y va hacia el
  pillager más cercano (que brilla y corre hacia él) y lo monta. Si no hay ninguno cerca, el dragón se desata y
  pelea con su aliento.
- Uno de cada cuatro reinos tiene una élite: dragón de hielo de fase 4 con armadura de netherita, montado por el
  Señor de la guerra (80 de vida, armadura, resistencia, no se baja hasta morir). Usa la escarcha del dragón:
  ventisca sobre ti, prisión de hielo y rugido helado (avisos en pantalla).
- No se domestican, no rompen bloques y su fuego no daña a los illagers. Al matarlos sueltan la armadura; el cuerpo
  solo da escamas.
- Armadura de netherita para dragones: en la mesa de herrería, armadura de diamante + plantilla de mejora + lingote
  de netherita.
- Sello del jinete (escamas de dragón, bloque de oro, hueso de dragón): llevándolo, tus dragones no te dañan.

## Ligadura de las hadas (tecla H, magia con V)

- Una hada por color: rosa fuerza, morada salto, azul velocidad, verde suerte, amarilla prisa. Para ligar: un hada
  domesticada de ese color cerca, abrir el menú y "Ligar". Solo una afuera a la vez ("Invocar" / "Guardar").
- "Abrir" (o clic en el hada) abre su menú: nivel, escudo, lo que te da ahora ("Da: Fuerza II" o, en niveles 4 y 5,
  "Da: +6 Daño de ataque"), sus 5 orbes (uno de cada tipo, todos a la vez), "Darle" (polvo de hada = experiencia;
  cosas de su color = hechizos), su Ayuda y su Magia.
- Cada color pelea distinto. Ayuda (sola, lenta, una por color): rosa puñetazo de luz, morada elevar, azul rayo de
  escarcha, verde sanación, amarilla chispa. Va contra lo que golpeas, lo que te golpea o los monstruos que van por
  ti; lo que mata cuenta para que suba.
- Magia: tres hechizos por color, se aprenden dándole una cosa y se usan desde un nivel; el elegido en el menú se
  lanza con V (junto a la barra rápida se ve el hechizo y su espera):
  - Rosa: Furia (polvo de blaze, nv 1), Onda de choque (hueso de dragón, nv 3), Pilar de fuego (sangre de dragón de
    fuego, nv 5).
  - Morada: Destello (perla de ender, nv 1), Pozo de gravedad (membrana de phantom, nv 3), Lluvia de estrellas
    (fragmento de amatista, nv 5).
  - Azul: Lanza de hielo (hielo azul, nv 1), Prisión de hielo (cubo de nieve polvo, nv 3), Ventisca (sangre de dragón
    de hielo, nv 5).
  - Verde: Raíces (flor de esporas, nv 1), Bendición (ambrosía, nv 3), Renacer (tótem de inmortalidad, nv 5: un
    minuto en que, si mueres, vuelves con media vida; espera 5 minutos).
  - Amarilla: Rayo (lingote de cobre, nv 1), Sobrecarga (piedra luminosa, nv 3), Tormenta de chispas (sangre de
    dragón de rayo, nv 5).
- Las ligaduras de antes de este cambio empiezan sin hechizos aprendidos (las habilidades viejas ya no existen).
- Niveles 1 a 3 dan el efecto; 4 y 5 dan estadísticas sin el efecto. Sube con lo que matas cerca de ella y con polvo
  de hada (en su menú o en el altar de hadas): un stack más por nivel (nivel 2: 1 stack de polvo; nivel 3: 2 stacks
  más; nivel 4: 3 más; nivel 5: 4 más).
- El hada ligada no recibe daño: tú recibes 30% (nivel 1) a 5% (nivel 5), después de su escudo de hada.
- Al darle polvo solo toma lo que le falta para su último nivel (el resto queda en la casilla); al máximo no toma nada.
- Santuarios de hadas (bosques): un árbol gigante (cerezo, roble viejo o abedul según el bosque) con el altar en el
  hueco de su base, rampa en espiral hasta una plataforma con casitas, y alrededor la aldea con caminos y faroles.
- Cuevas de hadas (bajo tierra): un túnel ancho con faroles baja desde un arco de flores hasta una aldea iluminada
  (plaza con el altar, casitas con caminos, arroyo con puentes, setas gigantes, cristales o azaleas). Sin lava.

## El hada desterrada (tecla J; su memoria con K)

- El primer árbol que corta cada jugador (un tronco con hojas crecidas alrededor; un tronco puesto a mano no cuenta)
  era la casa de un hada: sale enojada ("¡Eh! ¡Un enemigo ataca mi casa!", "¡Rayos! Malograste mi casita. Ahora
  tendrás que hacerte cargo de mí."), brilla y revolotea delante hasta que le hablas (clic derecho o J): "Cuidaré de
  ti". Desde ahí va contigo toda la aventura. Se puede apagar en config/iceandfire_fabric.json (`exiledPixie`).
- Es una de 20 (Albia, Brisa, Ceniza, Lumi, Selene, Opala, Nácar, Bruma, Escarcha, Marfil, Lirio, Rocío, Alba, Perla,
  Vela, Hiedra, Sauce, Cirro, Azahar, Ondina): el diseño de las hadas, desteñido a medio blanco (el color se le va
  hacia el borde del vestido y las puntas de las alas). Una por mundo y por jugador, no se cambia; ninguna se repite
  hasta que salieron las 20 (config/iceandfire_fabric/exiled_pixies.json lleva la ronda). Texturas: tools/gen_exiled_pixies.py.
- No recibe daño, no se guarda con el terreno (la vuelve a crear su jugador donde vaya: otra dimensión, al
  reconectar, al reaparecer). Va sobre tu hombro izquierdo, un poco atrás (no tapa la vista); cuando habla vuela
  delante de ti; con una misión vuela adelante hacia el lugar y espera encima.
- Habla arriba a la izquierda (su cara, su nombre, el texto escribiéndose) y la burbuja se va; cada línea se queda de
  5 a 10 segundos según lo larga que sea (algo menos si otra espera), para leerla con calma. Las líneas de misión y
  las alarmas quedan también en el chat. No hay panel fijo al costado.
- Su checkpoint (como el de las misiones de ShinColle, y puede estar a la vez que ese u otro): un haz de luz de su color
  en el lugar de la misión (sobre su farol cuando lo encendió; en el camino si está más lejos de lo que ves) y un pin
  con sus alitas sobre el lugar con la distancia debajo; si no está a la vista, el pin espera en el borde de la pantalla
  con una flecha. El cofre que muestra (su memoria, el de las brujas) tiene su propio pin dorado. Para quitarlo: en su
  menú, "Otro día" ("He vivido mil años, no tengo prisa.").
- Cada cuánto habla sola: poco, normal o mucho (en su menú). Comenta la primera noche, la nieve
  ("Recuerdo una niña marcada por el hielo..."), el desierto, el mar, la selva, el Nether, el End, un hada salvaje, un
  arma de hueso de dragón; avisa al entrar a una aldea o a un reino saqueador, con poca vida, con un dragón salvaje
  cerca, y si hay un cofre escondido muy cerca (vuela a él). Sus frases: "No todos pueden ser grandes aventureros,
  pero todos pueden ir de aventuras", "Un hada sin alas es un dragón sin escamas", "Cuando un hada pierde a su dueño,
  pierde el color...", "No pienso cometer el mismo error dos veces" y otras.
- Comenta las criaturas que andan cerca (a 24 bloques): cuanto más cerca, más a la vista, más de ellas y recién
  llegadas, más probable; las de su jugador, menos; de una misma clase no vuelve a hablar en 5 minutos. Mira cada 5
  segundos y deja al menos 45 s entre comentarios (2 min si habla poco, 25 s si mucho). Seis frases para cada una de las
  más comunes (zombi, esqueleto, creeper, araña, enderman, vaca, oveja, cerdo, gallina, caballo, lobo, gato, gólem de
  hierro, slime, ahogado, phantom, saqueador, abeja, zorro, conejo, calamar, murciélago, llama, vendedor ambulante;
  dragones de fuego, hielo y trueno, sirenas, cíclopes, troles, hadas, cocatrices, los muertos del terror,
  hipogrifos) y tres para el resto de vanilla y de Ice and Fire (hipocampos, gusanos de la muerte, gorgonas, aves de
  Estínfalo, serpientes marinas, fantasmas, hidras, myrmex, anfípteros, huevos de dragón, estatuas, esqueletos de
  dragón, dragones de hueso). Aldeanos: 50 anécdotas; brujas: 30 bromas. No repite una frase de un grupo hasta haberlas
  dicho todas. Las frases están en tools/exiled_lines_data.py (tools/gen_exiled_lines.py las pasa a los idiomas).
- En camino a la aldea cuenta anécdotas de aldeanos; en camino a la cabaña de las brujas, bromas de brujas (cada
  minuto y algo; 2-3 min si habla poco, 40-60 s si mucho).
- Cuando ligas otra hada (H), un guiño: "Últimamente todas se especializan en una cosa. En mis tiempos éramos buenas
  en todo.", "En mis tiempos el nivel 5 era lo básico...", "La magia ha cambiado. Ahora están de moda el hielo y el
  fuego.", "Dragones del trueno, una especie rara. Solo faltan dragones venenosos, ¡qué gran chiste!" y otros.
- Su menú (J o clic derecho): su retrato, nombre, "Un hada con más de mil años de edad. Su conocimiento es vasto; sus
  intenciones de usarlo, no tanto.", su lazo. Hablar (cuéntame algo, acompáñame / descansa, cuánto habla, guíame),
  Misiones (todas: hecha, superada, en curso, propuesta, disponible, más adelante; aceptar, ahora no para la que
  propone, abandonar) y Dones.
- Dones: con ella fuera y cerca, una parte de los dones de todas las hadas juntas en su mejor nivel: 10% con lazo 1,
  50% con lazo 5 (ataque +0,9 a +4,5, velocidad +5% a +25%, salto, caída segura, suerte, minado y velocidad de
  ataque). El lazo sube con sus misiones y con los enemigos que vences cerca de ella. Descansando no da nada.
- Su habilidad, Memoria de mil años (K o su menú): recuerda los cofres nunca abiertos a 32-64 bloques (según su lazo):
  una columna de luz sobre cada uno, vuela al más cercano y la pantalla marca dirección y distancia. Espera 5 minutos
  (lazo 1) a 1:40 (lazo 5).
- Su anillo (el punto de control de cada lugar): al acercarte (unos 70 bloques) enciende su farol en el lugar, un
  cristal de su color sobre piedra con musgo, en un círculo de velas encendidas de su color, con una columna de luz que
  ves desde lejos ("Encendí mi farol allí. ¡Ve hacia la luz!"; en pantalla "Su farol · 12 m"). Lo pone en tierra
  firme y natural junto al lugar (nunca sobre un techo ni rompiendo lo construido: solo reemplaza aire y plantas), a
  la altura del suelo del lugar (no al pie de un acantilado ni en un barranco al lado) y nunca encerrado (un corral:
  caminando desde ahí tiene que haber salida); junto a un esqueleto o un rebaño, a un lado (no debajo). Pisar el anillo es llegar: una visita queda hecha y en las
  demás te dice qué hacer. Cumplida la misión, el anillo queda como recuerdo (cristal gris, velas apagadas); si
  abandonas la misión, se quita. Si lo rompes (o se rompe), no lo vuelve a poner: llegar cerca del lugar basta. Sin
  suelo posible, igual: llegar cerca basta.
- Ni ella ni el cuerpo de un dragón muerto estorban al picar: si están bajo la mira, el golpe pasa a través y rompe el
  bloque de detrás (antes le pegaba a ella o al cuerpo y no se podía romper nada ahí). El clic derecho sí les llega:
  hablarle, sacarle los huesos a un esqueleto.
- Misiones (las propone de a una, nunca dos veces; "ahora no" la deja 10 minutos; en el menú puedes tomar cualquiera
  disponible; si ya tienes lo que pide, cuenta hecha al instante; si ya la pasaste, se salta sola). Cada una es un
  recuerdo suyo que vuelve, y cuentan aunque las hagas sin ella: tener lo que piden (tarta, huesos, ojo, huevo, un hada
  tuya), estar dentro del lugar (una aldea, un cementerio, el mausoleo, el templo) o haberlo hecho alguna vez (vencer a
  un trol o a una hidra y la distancia a caballo, según las estadísticas del juego; montar un hipogrifo; un hada tuya a
  nivel 5). El menú (que ahora se desplaza con la rueda) dice cuántos recuerdos volvieron (N de 14); con todos, "Ya lo
  recuerdo todo..." y estás listo para Cryona:
  1. Una aldea cerca: te lleva a la aldea más cercana que de verdad generó el mundo (cualquier tipo), contándote
     anécdotas de aldeanos. Su anillo va junto a la campana de la aldea (donde se reúne su gente); llegar es hecha.
  2. Una tarta para mis amigas (tras la aldea): "Conozco unas brujas por aquí. Tienen tartas. Muchas tartas." Te lleva
     a la cabaña de brujas más cercana, con bromas de brujas por el camino; su anillo va en el claro, del lado por el
     que llegas; al llegar te muestra su cofre (pin dorado,
     vuela a él): está lleno de tartas (5 a 8). Con una tarta en la mochila queda hecha. Se salta si ya tienes un hada
     ligada.
  3. Mis amigas del bosque: lleva la tarta al santuario del gran árbol o a la cueva de musgo de las hadas (el más
     cercano de los dos; en la cueva te lleva primero a su entrada, el arco de abedul, "Es aquí abajo. Sígueme.", y
     luego a la plaza con las hadas; su anillo allí abajo). Al llegar: sus amigas ya no están ("Mis
     amigas... ya no están. Vámonos pronto de aquí...", "Es normal. Pasó mucho tiempo..."), las hadas la miran raro
     ("No me gusta cómo me miran...") y "lanza una tarta (Q) y cualquiera querrá ser tu amiga. Así de fáciles son".
     Suéltala cerca de una: queda tuya.
  4. A caballo (tras la aldea): montar un caballo (un burro vale). Te muestra el más cercano; si no hay, dónde pastan.
  5. Las tumbas antiguas: llegar a un cementerio.
  6. Huesos de dragón: un esqueleto (el más cercano que se generó; si no hay ninguno a 900 bloques, ella "recuerda"
     uno a 120-260 bloques en tierra seca, que aparece cuando te acercas): 4 huesos (clic derecho con la mano vacía).
     Se salta con espada/hacha de diamante o mejor, maza, armas de hueso de dragón o dragonsteel, o 3 piezas de
     armadura de diamante o mejor.
  7. Un hada completa (tras sus amigas): un hada tuya a nivel 5 (polvo de hada en su menú, H).
  8. El trol del camino (tras los huesos): vencer a un trol; te muestra el más cercano o dónde salen.
  9. El ojo del cíclope (tras los huesos): un cíclope (cueva o con su rebaño; si no hay, ella recuerda uno con ovejas):
     hiere una oveja y viene; vencido en esta misión siempre deja su ojo.
  10. Alas prestadas (tras el caballo): montar un hipogrifo (conejo para ganártelo, y una silla).
  11. El mausoleo; 12. Un huevo de dragón: a una cueva de dragones donde una hembra adulta (etapa 4 o 5) guarda sus
     huevos con su pareja (se salta si ya tienes un dragón propio); 13. El templo de la gorgona; 14. La hidra del
     pantano (tras el cíclope): vencer a una hidra; te lleva a la cueva de hidra más cercana (ahora es una estructura:
     la encuentra como /locate, también las que aún no se generaron, hasta 6000 bloques; las de mundos viejos se
     anotan al generarse) y, si no hay ninguna, a la hidra más cercana. 15. "???": "Aún no es el momento. Cuando recupere todos mis recuerdos y la
     encuentres, te prestaré mi fuerza." No se propone: llega sola (ver abajo).
     Recuerdos que vuelven al hacerlas: un caballo blanco y una niña riendo; un trol que les cerró el paso; volar con
     ella en un hipogrifo; cómo era ella antes, entera; la hidra de la que escapó la niña ("el hielo la salvó: ahí
     empezó todo").
- El momento adecuado: cuando encuentras a Cryona (a 40 bloques) con todos sus recuerdos de vuelta, tu hada te detiene
  ("¡Espera! Es ella...") y te lleva a su claro, un mundo aparte (una isla flotante con un cerezo, flores, casas de
  hadas y su anillo bajo un cielo pastel). Allí te cuenta que Cryona es la niña marcada por el hielo, que esta vez no
  cometerá el mismo error, te pide "Cierra los ojos..." (la pantalla se va a negro), te da lo más preciado de su vida
  pasada, la Resistencia al hielo absoluto (lo oyes a oscuras), y los ojos se abren de a poco: una rendija de luz que
  se ensancha, un parpadeo pesado, y el claro aparece. Luego vuelves donde estabas. La misión se llama entonces "El
  momento adecuado" y queda hecha; el don figura en Dones.
- Todo eso es un recuerdo, visto como una película: desde su llamada entran barras negras arriba y abajo, no queda
  nada más en pantalla (ni barra de objetos, vida, mira ni chat; tampoco tus manos) y sus frases son los subtítulos,
  en la barra de abajo. Cierras los ojos al seguirla y los abres en su claro, que tiene los bordes brumosos; al final
  los cierras otra vez y los abres donde estabas, y las barras se van. El nombre del don aparece en grande en el medio
  cuando abres los ojos. No se puede salir mal: no te mueves ni giras la vista (te quedas donde estás, no puedes caer
  de la isla ni al vacío), nada te hace daño, no se rompe, pone, usa ni golpea nada, el inventario no se abre, lo que
  tires vuelve a tu mochila al instante (si está llena, te lo devuelve al volver), y sus otros comentarios esperan a
  que termine (también los avisos de logros o recetas). Tu hada ligada no entra (el recuerdo es de ella): te espera
  donde estabas. Solo empieza cuando estás de pie en el suelo (ni montado ni volando). Esc sigue pausando. Si sales del
  juego en medio, al volver apareces donde estabas.
- Un hada ligada que vuelve a ti (al cambiar de mundo o si quedó lejos) aparece detrás de tu cabeza, no en tu cara.
- Si aún te faltan recuerdos, cerca de Cryona te avisa ("¡Espera! Aún no estás listo para esta batalla." y "Me faltan
  N recuerdos. Sin mi don, su hielo irá directo a tu corazón y no podrás curarte."), cada 5 minutos como mucho, y no
  te frena: la pelea es tuya.
- El aliento de Black Frost, sin el don del hada, es mortal: te derrite (cada pocos ticks dentro del chorro pierdes el
  10% de tu vida, el 14% con su súper aliento, directo al corazón y cauterizado) y te congela. Con el don es un golpe
  de frío fuerte, igual en toda su franja: cada vez que te alcanza te quita el 8% de tu vida (el 12% su súper
  aliento); la armadura no lo frena, pero tus encantamientos, pociones y escudo sí; no te cauteriza. Con o sin el don,
  te deja congelado como en la nieve polvo (escarcha en la vista, tiritas). La poción Frost Ward lo baja y evita esa
  escarcha.
- El frío de Black Frost: su aliento congela siempre a quien alcanza (un par de segundos libre después para salir de
  él); la poción Frost Ward lo anula. Su Prisión Glacial y el Cero Absoluto de Cryona congelan a través de la poción.
  Con la Resistencia al hielo absoluto: el primer toque del aliento solo enfría, si te vuelve a alcanzar te congela la
  mitad de tiempo, te recuperas antes, todo hielo de ellos te retiene la mitad, y el Cero Absoluto te quita la mitad de
  la vida en vez de nueve décimas.
- Sin ese don, el hielo de Black Frost (su aliento, sus golpes de hielo, su picado, su prisión) y el Cero Absoluto de
  Cryona van directo a tus corazones: nada lo frena (armadura, encantamientos, Resistencia, la poción Frost Ward,
  escudos, corazones dorados, ni la invencibilidad de otro mod; solo el modo creativo o la marca de invulnerable del
  propio juego, que solo ponen las pruebas; un tótem de la inmortalidad en
  la mano sí te salva de la mordida que te mataría, y se gasta), y te cauteriza: 20
  segundos sin poder curarte con nada (comida, pociones, manzanas, ni otro mod; la leche quita el icono, no la herida),
  renovados con cada mordida. Cada mordida te hace estremecer una vez por medio segundo como mucho (no un sonido de
  daño tras otro). Si caes, caes de verdad: tus
  cosas te esperan en la arena y el duelo queda en pausa, una lección. "Cauterización helada" se ve como efecto (un
  corazón medio congelado; icono de tools/gen_frost_wound.py).
- La barra de jefe del duelo (Cryona sobre Black Frost) ya aparece: antes, mientras la pelea guiaba a Black Frost, su
  barra no se actualizaba y la de Cryona se oculta mientras va montada.
- Cabañas de brujas (`witch_cottage`): pequeñas, en bosques, taigas, pantanos, llanuras, praderas, cerezos y sabanas
  (una cada 16 chunks como mucho, nunca a menos de 5 chunks de una aldea), en un claro sobre tierra firme. Una choza de
  abeto y roble oscuro sobre piedra con musgo, un escalón, techo empinado como sombrero de bruja (ala de roble oscuro,
  copa de lana morada, farol de almas en la punta) y chimenea humeante; dentro un caldero, un soporte de pociones,
  estanterías, una tarta mordida sobre un barril y el cofre de tartas (también azúcar, galletas, pastel de calabaza,
  frascos, polvo de piedra luminosa, redstone, ojos de araña, a veces polvo de hada). Fuera, calabazas y setas. Las
  brujas no están.
- Cuevas de hidra (`hydra_cave`): ahora son una estructura (antes un adorno del pantano que nadie podía buscar):
  pantanos y manglares, una cada 10 chunks como mucho, a más de 1000 bloques del inicio del mundo, en suelo bastante
  plano o sobre agua poco profunda (el agua bajo el montículo se vuelve piedra: una isla, como siempre). La misma cueva
  de siempre: un montículo de pasto con cuatro entradas, el hueco con cofres, cráneos, hojas y setas, y la hidra
  dentro. /locate structure iceandfire_fabric:hydra_cave y /iceandfire find hydra la encuentran.
- Busca los lugares de a poco (dos sitios por tick, como /locate pero sin trabar el juego) y nunca genera terreno
  lejano; los esqueletos, cíclopes y nidos se anotan al generarse (se guardan en el mundo, en data/iafabric/iafabric_landmarks).
- Para probar (con trucos): /iceandfire fairy reset (olvidarla: el próximo árbol trae otra), /iceandfire fairy meet, y
  /iceandfire fairy complete: el hada desterrada queda tu amiga (la conoces al instante si no), con sus 14 misiones
  hechas y su lazo a nivel 5, y un hada ligada a nivel 5 con sus tres hechizos y escudo lleno, contigo: la de Fuerza
  (más daño) por defecto, u otra con `complete jump|speed|luck|haste`. El momento adecuado queda por llegar (si ya
  tenías su don, vuelve a dártelo al encontrar a Cryona).

## Huevos y cuevas de dragones

- Solo las hembras adultas (etapa 4 o 5) ponen huevos: crían con un macho adulto de su especie (los dos de etapa 4 o
  5), y solo el cuerpo de una hembra adulta puede soltar un huevo.
- Cada cueva de dragones (etapa 4) tiene una pareja que cría: una hembra de etapa 4 o 5 con uno o dos huevos a su lado
  y un macho; a veces un tercero, un anciano.
- Los dragones del terror (los que montan los caballeros en sus agujas) son dragones de hueso: sin piel, alas de solo
  hueso, ojos de fuego verde. Nada que ver con Black Frost (escamas negras, hielo azul).

## El terror

- Agujas del terror (frío): torre negra con un dragón no-muerto en la cima y otro en un nido de huesos, cada uno
  montado por un caballero del terror (dibujado en el lomo, como los pillagers); dos caballeros a pie en el patio;
  tumbas, liches y no-muertos. Los dragones del terror no rompen bloques y su aliento no daña ni congela a los suyos.
- Ciudadela de Cryona (muy rara: una cada 160 chunks como mucho, en la nieve y en tierra amplia y seca). Murallas
  oscuras con torres redondas, un portón con el rastrillo alzado, un patio de tumbas, braseros y obeliscos de hielo
  guardado por muertos, y al fondo la torre del homenaje: el gran salón (pilares, faroles de almas colgando, un liche
  ante el arco) y la sala del trono bajo un techo alto, hecha a la medida de Black Frost (mide unos 50 bloques de las
  fauces a la cola y unos 40 de ala a ala; sentado, unos 44 de ancho, 37 de fondo y 13 de alto): Cryona duerme en su
  trono cerca de la entrada y Black Frost está sentado y erguido detrás de ella, en su lecho de hielo azul, mirando
  hacia la entrada y girando la cabeza hacia quien entra; su cabeza asoma sobre el trono como una corona y sus alas
  caen a ambos lados. Se recorta contra un muro de hielo iluminado al fondo, con lámparas colgadas a su altura. Nada de
  él atraviesa paredes, pilares ni lámparas, y del arco al trono el pasillo queda libre. El gran salón quedó algo más
  corto para darle ese fondo a la sala. Al entrar a sus terrenos se oye a Cryona desde dentro.
  - En un mundo viejo, la torre del homenaje (con el trono al fondo de una sala pequeña y Black Frost tumbado en medio,
    tapando el paso, con las alas atravesando los pilares) se rehace sola como la nueva la primera vez que se carga con
    alguien cerca, unas filas cada tick para no parar el juego; el cofre de detrás del trono se muda con lo que tenía, y
    quien quede dentro de un muro nuevo aparece en el arco.
  - Mientras ella reine, ninguna piedra se rompe y no se puede poner nada dentro (ni bloques, ni lava o agua con cubo,
    ni fuego, barcos, vagonetas o criaturas); ni las explosiones ni los dragones la dañan (en creativo sí se puede
    construir): hay que entrar por el portón.
  - En su reino, el Trono Helado, no se puede poner ni romper nada nunca (salvo en creativo): solo pelear. Al vencerla, el frío que la
    sostenía se rompe y es piedra como cualquier otra.
  - Cómo llegar: los escribientes (aldeanos que trabajan en el Atril del Bestiario: un aldeano sin trabajo junto a un
    atril se vuelve escribiente) venden el "Mapa a la Ciudadela
    Helada" (aprendiz: 16 esmeraldas y una brújula; una X roja la marca). Con trucos: `/iceandfire find throne`.
- La Cryona dormida y el Black Frost de la ciudadela son su señal, criaturas propias ("Cryona, dormida en su trono" y
  "Black Frost, guardián de su trono"), no ellos mismos: nunca pelean y nada los daña; ninguno se mueve de su sitio
  (él solo gira la cabeza). Black Frost no es de nadie (no se monta, no se doma, no se ata) y, si falta, ella lo
  vuelve a poner. Los dos de verdad esperan en su reino. En un mundo viejo, los que dormían allí se cambian solos por su señal
  al cargarse.
- Al llegar al pie de su trono (a 7 bloques de ella), o si golpeas a cualquiera de los dos, su
  llamada te arrastra (a ti y a los jugadores a 40 bloques) a su reino: el Trono Helado, una dimensión aparte con una
  arena inmensa de hielo (radio 120, pilares, obeliscos para cubrirse, un bosque de picos en el borde; si te alejas
  demasiado el frío te devuelve). Solo ustedes y ella. Si su llamada no puede ser, ella solo sigue durmiendo (nunca despierta a pelear en
  el mundo normal).
- Al llegar no hay pelea todavía: Cryona y Black Frost no atacan ni reciben daño, solo hablan. Lo que te mira es un
  fragmento de la verdadera Cryona, atado a su trono, que pondrá a prueba si eres digno de sus armas.
  - El fragmento está de pie pasado el altar, mirándote. Black Frost ronda detrás de ella: camina un arco a su
    espalda (a 23 bloques, para que sus alas nunca pasen sobre ella), se da la vuelta en cada extremo y vuelve, con la
    cabeza siempre hacia ti. Nunca se pone entre tú y ella
    (el haz del altar son dos, a sus lados, que la enmarcan). Puedes hablarle (clic derecho): te muestra lo mismo que
    el altar. Al empezar el duelo, ella salta a su lomo.
  - Un camino de luz lleva al Altar del Trono Helado. Al llegar a él (o con clic derecho) se abre la tarjeta del
    duelo, como en los juegos antes de un jefe: dos pestañas (I · Black Frost, II · Cryona), cada jefe dibujado entero
    y centrado, mirándote, su vida (y el escudo de ella) en barras, sus poderes como iconos con su nombre y cómo
    esquivarlos en una línea (los que quitan el 90% en rojo), cuatro marcas con las reglas (rojo = 90%, caer no cuesta
    nada, 1% de su vida por segundo, aturdido: golpea) y lo que pide: 1 sangre de dragón de hielo.
  - Al señalar un poder, una regla o un botón, su explicación completa sale en una franja fija de la tarjeta (nada
    flota encima). La tarjeta se achica sola si la pantalla es pequeña o la escala de interfaz grande: nunca se corta
    ni se pisan los textos, en inglés ni en español.
  - Botones: "Luchar" (gasta la sangre y la pelea empieza; apagado si no tienes), "Volver" o "Pensarlo". La luz a tu
    espalda también te lleva a casa. No hay límite de tiempo para decidir.
  - Si vuelves a casa sin aceptar, apareces donde estabas y su trono no te arrastra otra vez (ni ella despierta por
    ti) mientras sigas cerca. Cuando te alejes (unos 56 bloques) y vuelvas, te llevará de nuevo a la misma arena; si
    la golpeas en su trono, te lleva al momento.
- Si caes (o te vas) y no queda nadie peleando, el duelo queda en pausa: los dos se quedan quietos como estaban (su
  vida no se cura) y tus cosas se quedan en el suelo del reino, sin desaparecer nunca. Ella vuelve a dormir en su
  trono de la ciudadela; al acercarte, te lleva a la misma arena: recoges tus cosas y en el altar ofreces otra sangre
  para seguir donde quedó. Si otro jugador sigue peleando, quien cayó puede volver al trono y unirse.
- Jefes de verdad (la pelea dura al menos 100 s):
  - Black Frost tiene 10 000 de vida; Cryona 3000 y un escudo de hielo de hasta 1000 (su barra blanca).
  - Ningún golpe les quita más del 1% de su vida en un segundo, venga de donde venga (solo /kill y el vacío).
  - Sus golpes te quitan un porcentaje de tu vida sin importar armadura, encantamientos, pociones ni escudo. Los de
    Cryona tampoco los absorben los corazones dorados (van a tu vida de verdad; los dorados se quedan).
  - Los que más tardan en cargarse te quitan el 90% de tu vida máxima si no los esquivas, siempre: el centro rojo del
    picado de Black Frost (su círculo naranja duele menos), el rayo de Cryona y el Cero absoluto fuera de las luces.
    Los corazones dorados no lo absorben (se quedan). Si ya estabas herido te mata; un tótem en la mano te salva.
  - Black Frost es un muro: ningún golpe ni empujón lo mueve, y empuja a quien se le pega.
  - El escudo de hielo suena al alzarse (cuando empieza a repararse tras estar bajo, al repararse tras alzar muertos,
    al levantarse ella) y al romperse (sonidos hechos con ElevenLabs); al golpearlo suena como mucho una vez cada
    0,4 s, no a cada golpe.
  - El escudo de hielo recibe los golpes primero. Se llena cuando ella se alza sola, se repara si nadie la golpea
    8 s y después de alzar muertos. Mientras más la golpeas, antes se recupera de estar agotada y menos se repara.
- Un solo jefe en dos partes. Cryona habla en una lengua que nadie conoce (se oye su voz y se escriben sus palabras):
  al entrar a la ciudadela, al despertar, en sus hechizos, al llorar a Black Frost, al jurar venganza, al flaquear y
  al morir.
  1. Cryona montada en Black Frost: una sola barra ("Cryona, sobre Black Frost", la vida de Black Frost). Un golpe a
     ella le llega a Black Frost. Ella lanza desde el lomo: círculos de escarcha con picos, bloques de hielo que gira y
     lanza por líneas marcadas, a veces alza 2 muertos (duran 30 s, como mucho 3 a la vez, no sueltan nada) y, de
     lejos, calaveras de hielo (cada 12 s como mucho). Black Frost: embestida (franja roja; contra un pilar u obelisco
     queda aturdido), picado (círculo rojo; aturdido después), granizo, rugido glacial, su aliento. A la mitad de su vida, Cero
     absoluto: Black Frost vuela con ella sobre el centro (escudo) y hay que meterse en una de las tres luces naranjas;
     luego aterriza aturdido y ella queda agotada 7 s.
     - Black Frost cambia de forma de pelear cada 15-30 s, como algo vivo, y su cabeza siempre te sigue:
       - De lejos: se posa en uno de sus posaderos, seis puntos marcados en el hielo de la arena (un anillo de piedra
         del terror con ocho luces y una estrella de hielo azul; el primero es el círculo del centro), en terreno
         abierto, donde se le ve entero (y a ella en su lomo). Desde ahí: andanada (tres esquirlas de hielo que caen
         donde se marca un círculo azul), granizo, prisión, su aliento.
       - Cuerpo a cuerpo: baja y corre hacia ti y golpea; cada golpe se avisa con un círculo naranja: mordisco ante
         su cabeza, coletazo alrededor de su lomo y costados, pisotón ante sus patas (te frena), y su aliento de cerca
         si estás delante (a sus lados te salvas). Tras cada golpe
         descansa un momento: ahí puedes pegarle. Si estás muy lejos, salta sobre ti (su picado). Tras un rato (o tras
         perder algo de vida) alza el vuelo.
       - Estampida: tres cargas de punta a punta de la arena, cada una por un carril rojo que pasa por ti. Contra un
         pilar u obelisco queda aturdido; tras la tercera se queda sin aliento (aturdido) y sigue cuerpo a cuerpo.
       - Vuela de un posadero a otro en una curva amplia alrededor de la arena, alto sobre los obeliscos (un círculo
         azul marca dónde aterriza) y aterriza mirándote.
     - Aura helada (de lejos, en su posadero): cerca de él te frena (más cuanto más cerca) y la vista se te escarcha;
       cada 2 s una ráfaga de hielo te echa atrás. Aturdido no tiene aura.
     - Si llegas a él cuando pelea de lejos: ruge (te echa atrás), usa las garras, baja a pelear contigo o, a veces,
       vuela a otro posadero.
     - Garras: un círculo naranja de 4,5 se cierra a tus pies (1 s). Si sigues dentro te alza en sus garras y te lanza
       lejos, hacia el centro de la arena. Si lo golpeas de cerca cuerpo a cuerpo (en cualquier parte de su cuerpo, y
       aunque el golpe no le haga daño, como con la debilidad que deja su rugido), sus garras van por ti en cuanto
       estén listas (cada 7-9 s): pegarle de cerca tiene su precio. La caída no te quita nada por la altura; al dar en
       el hielo pierdes una parte de tu vida (12% al atraparte, 20% al caer). Mientras te tiene, puedes golpearlo. Si
       sales del círculo, falla y queda expuesto (aturdido) un momento.
     - Momentos para pegarle de cerca: cuerpo a cuerpo tras cada golpe suyo; aturdido (recibe la mitad más) tras su
       picado, un choque en la estampida, el final de la estampida, unas garras falladas y el Cero absoluto.
     - Cryona va sentada en su lomo tal como se dibuja él: gira, se inclina y sube y baja con su cuerpo al volar.
     - Prisión glacial (rara, cada 75-105 s): un gran círculo azul oscuro se llena despacio (3 s) alrededor de un
       jugador. Quien siga dentro al llenarse queda congelado de verdad 4 s, con o sin poción. Ellos dos lo dejan en
       paz mientras tanto.
     - Aliento (en cualquier momento, de lejos o de cerca): toma aire mientras una franja roja marca hasta dónde llega
       (hasta ti y un poco más), y un chorro de escarcha baja de sus fauces barriendo la franja hasta el suelo, con
       niebla y copos por todo su recorrido: dentro, lo ves venir encima. Sal de la franja (a los lados o detrás).
     - Súper aliento: cada unos 28 s (cada 19 s con menos de 30% de vida), sea como sea que esté peleando: se alza y
       lo suelta casi en seguida (1,1 s), más ancho (11), más largo (hasta 56) y soplando más tiempo.
     - Pilones (desde que le queda 75% de vida): salen tres cristales de hielo azul alrededor de la arena, lejos de
       él, cada uno con una luz al cielo, su círculo y esquirlas al pie. Tres golpes rompen uno (se agrieta y baja con
       cada golpe) y su luz corre hasta Black Frost: pierde 5% de su vida de una vez (más allá del 1% por segundo,
       nunca lo mata). Duran 40 s; salen otros 45-55 s después de que no quede ninguno.
     - Sus rugidos no se pisan: como mucho uno cada 6 s (suenan fuerte y largo).
     - Embestidas: la de lejos pasa por ti y sigue unos 30 bloques más (hasta el borde de la arena como mucho), muy
       rápida; al arrancar y al frenar revienta el hielo, y por el camino levanta hielo y nieve a sus patas y deja a los
       dos lados esquirlas de hielo que brotan inclinadas hacia fuera y se deshacen al rato (también en la estampida).
     - Áreas más grandes: el picado cae en un círculo rojo de 11 y todo él te quita 3/4 de tu vida (en el borde igual
       que en el centro; los corazones dorados no lo absorben), el rugido glacial llega a 16, el granizo y la andanada
       tienen círculos más anchos.
     - Granizo: en dos oleadas, nunca un círculo encima de otro. Primero un anillo de siete círculos alrededor de ti y
       uno a tus pies (te salvas entre los dos o saliendo por un hueco); luego, donde fuiste, un anillo más cerrado y
       otro a tus pies, más rápido. Los círculos de picos y la lluvia de carámbanos de Cryona también caen en anillo.
     - Su cabeza va siempre con su cuerpo, derecha: para mirarte gira el cuerpo entero (de cerca su cuello ya no se
       tuerce hacia un lado ni hunde las fauces en el hielo; solo al soltar su aliento la baja un poco). Sus golpes
       caen donde llega su cuerpo: el mordisco a la distancia de sus fauces (unos 16 bloques delante), el pisotón bajo
       su pecho, el coletazo detrás. Su cuello nunca cuelga: en el pisotón (su aletazo) se alza con el pecho y el cuello
       arriba (antes la cabeza le colgaba frente al pecho), y si sigues pegado a su pecho retrocede unos pasos y te
       muerde, en vez de pisotear una y otra vez.
     - El hielo que se rompe a la vez suena una sola vez, no un estruendo de vidrios uno sobre otro.
     - El altar se rompe en esquirlas de hielo al empezar el duelo (no queda en medio de la pelea) y vuelve a estar
       cuando el duelo queda en espera, para la próxima vez.
  2. Black Frost cae: ella se arrodilla junto a él y lo llora; su cuerpo se deshace en niebla que va hacia ella y se
     hunde en el hielo (deja sangre de dragón de hielo, su corazón, huesos y escamas). Luego, su duelo (lo que dura la
     canción "Recuerdos del hada", 1 min 44 s): ella sigue de rodillas y nada la toca (golpearla lo muestra: su pena
     la protege); su barra roja, "La furia de Cryona", se llena con el tiempo (llena, se levanta); solloza en su
     lengua y de a poco se enfurece; una horda de muertos sale alrededor tuyo en oleadas (cada 10 s, 5 más cada vez,
     hasta 12 a la vez con un jugador; 2 y 4 más por cada otro jugador) y caen rayos de hielo del cielo: círculos rojos que se
     llenan con una luz fina encima y luego una columna de hielo que quita 40% de tu vida (más rayos a medida que se
     enfurece). Muy de vez en cuando un trueno cae lejos (solo luz y sonido). Al terminar la canción se levanta (su
     escudo entero, los muertos se deshacen) y vuelve la música del duelo. Si caes en su duelo, al volver empieza otra
     vez desde la canción.
     Se alza por venganza con su propia barra ("Cryona, la Reina del Terror") y pelea sola, todo avisado en el suelo:
     - Tres tajos: cada franja roja delante de ella se marca un momento antes del corte; el tercero (estocada) llega
       más lejos. Después se queda tomando aire (nubecitas): es el momento de pegarle (recibe 30% más).
     - Salto: se agacha y salta; un círculo rojo marca dónde cae. Al caer brotan picos alrededor y se queda clavada un
       momento.
     - Picos en franja: golpea el suelo con el báculo y los picos corren por tres franjas azules, uno tras otro.
     - Nova de escarcha (si estás pegado a ella), círculos de picos, paso por el frío detrás de ti (una runa avisa) y
       tajo en seguida, calaveras de lejos (pocas).
     - Golpe y salto atrás: un tajo corto y se aleja de un salto.
     - Rayo: una franja larga se marca y se llena 3 s, luego dispara por ella (mata: sal de la franja).
     - Desde la mitad de su vida, lluvia de hielo: círculos azules donde caen carámbanos del cielo.
     - Rayos en todas direcciones: seis franjas rojas salen de ella (una pasa por ti) y por todas corre su rayo a la
       vez; en seguida otras seis entre ellas. Lejos de ella, entre dos franjas, te salvas (35% de tu vida cada uno).
     - Anillo de hielo: un anillo de picos sale de ella hasta 22 bloques, salvo por tres huecos marcados con luces
       cálidas: sal por un hueco o sáltalo cuando llega.
     - Rayos de hielo del cielo (los de su duelo): un círculo rojo a tus pies y otros alrededor (30% de tu vida).
     - Más presión: sus golpes quitan más, descansa menos tras ellos, sus círculos son más grandes (salto 7, nova 11,
       picos 3,8) y habla menos seguido (nunca dos frases en 8 s).
     - Al 70% y al 35% alza 2 muertos por un rato. A la mitad y a un quinto de su vida, Cero absoluto (luces naranjas);
       después se arrodilla agotada 7 s: recibe el doble y alza de 5 a 8 muertos. Con poca vida ataca más seguido.
  3. Su final: se arrodilla, dice sus últimas palabras, su espada y su báculo se elevan y flotan hasta quien la venció
     (quedan en su inventario, o a sus pies si está lleno) y ella se desvanece en escarcha (suelta su botín).
- Sonidos (ElevenLabs): cada aliento de Black Frost suena a una carga mágica densa que estalla justo cuando sale el
  chorro, con eco, y dura lo que dura ese aliento (el súper aliento y el normal tienen el suyo); si el aliento se corta,
  su sonido también. En el recuerdo del hada, mientras se escriben sus palabras se oye una pluma escribiendo, suave.
- El hada en el duelo: solo habla de la pelea (sus demás comentarios esperan a que termine): avisa los golpes grandes
  (aliento, picado, estampida, garras, prisión, Cero absoluto, rayos, anillo), las aperturas (aturdido, agotada, escudo
  roto), los pilones, la caída de Black Frost, el luto de Cryona y si estás muy herido. Nunca dos frases seguidas ni
  la misma en un minuto; con "hablar poco" solo lo urgente.
- Música (el duelo y los recuerdos del hada): una sola canción a la vez, nunca encima de otra ni de la música del juego
  (la del juego espera mientras suena una del mod). El duelo tiene su canción en bucle desde que empieza; cuando
  Black Frost cae, se apaga de a poco y empieza "Recuerdos del hada" (hasta su final); al levantarse Cryona vuelve la
  del duelo; al morir ella se apaga. Si caes o sales del reino se apaga en seguida, y al revivir no vuelve. En la
  visión del hada (el momento justo) suena "Recuerdos del hada" desde su llamada y en su claro mientras te cuenta, y
  se apaga al volver. Se oyen con el volumen de Música (con la música en 0 no suenan, y empiezan si la subes).
- Armas de Cryona: si el jinete o el dueño de un dragón de hielo lleva su espada o su báculo en la mano, el hielo de
  ese dragón congela por completo, con o sin poción de escarcha (su descripción: "En mano: tu dragón de hielo congela
  del todo").
- La sala del trono de la ciudadela ahora tiene luz: filas de linternas de mar en los muros, un panel detrás del
  trono, dos filas de luz en el suelo del pasillo, lámparas colgando junto al trono y luces en el estrado (solo en
  ciudadelas nuevas).
- Cómo defenderse: todo se avisa en el suelo. Círculo azul que se llena (picos) o rojo (picado, salto, nova): sal
  antes de que se llene. Franja roja: sal de ella. Círculo azul oscuro grande (prisión): sal ya, tarda 3 s. Cero
  absoluto: corre a una luz naranja. El hielo es corto y justo: tras congelarte quedas 8 s libre de hielo, y al
  empezar el Cero absoluto se le quita el hielo y la lentitud a todos. El fuego también rompe el hielo.
- Poción de escarcha (Escudo de escarcha): el aliento de los dragones de hielo y la escarcha de Cryona y Black Frost
  no te congelan ni te ralentizan, y el aliento de hielo te hace 40% menos. No sirve contra la Prisión glacial.
  - Corta (20 s): la venden todos los escribientes desde novato, 2 esmeraldas (una pelea pide varias).
  - Larga (1 minuto): el escribiente oficial la vende por 3 esmeraldas y una sangre de dragón de hielo, o se destila:
    poción rara + sangre de dragón de hielo (también arrojadiza y persistente).
  - Los escribientes que ya tenían sus tratos no las ofrecen hasta subir de nivel; uno nuevo sí.
- Al vencerla se abre una luz en el centro: entrar en ella (o esperar un minuto) te devuelve a donde estabas, y su
  ciudadela queda libre. Si mueren o se van todos, ella dice unas palabras sobre ustedes y vuelve a dormir en su trono
  con Black Frost para otro intento.
- Huevo de prueba: "Huevo generador de la Reina del Terror" (sale despierta, sola, y pelea donde está con todo lo de la
  segunda parte; el reino se abre solo cuando la despiertas en su trono: `/iceandfire find throne`).

## Idiomas y pantalla de inicio

- Todos los textos están en los 16 idiomas del mod: inglés, español (España, Argentina, Chile, Ecuador, México),
  alemán, francés, japonés, coreano, polaco, portugués de Brasil, ruso, ucraniano, vietnamita y chino. Los textos que
  el original no tenía se tradujeron para el port (`tools/translations/<idioma>.json`; `tools/put_translations.py`
  los pone en los archivos de idioma y `tools/check_lang.py` revisa que no falte ninguno).
- Un texto más largo (alemán, ruso, francés) se achica solo lo justo para caber entero en su cuadro: la tarjeta del
  duelo, el menú del hada, las cartas de los lazos, la caja donde habla el hada y los subtítulos de su recuerdo. Las
  líneas sobre la barra de objetos y los títulos se estrechan si no caben en la pantalla.
- El bestiario (el libro) está en los idiomas del original: inglés, español, polaco (en parte), portugués, ruso,
  ucraniano y chino; las variantes del español leen el de España, y los demás idiomas lo leen en inglés.
- La pantalla de inicio es la del original: el bestiario abierto sobre una mesa, una página que pasa cada dos
  segundos con dibujos de sus criaturas, sus frases amarillas y "Ice and Fire" con su versión abajo. Se apaga con
  `customMainMenu: false` en `config/iceandfire_fabric.json` (vuelve el panorama del juego).

## Nombre del mod

- Se llama Ice and Fire Fabric (id `iceandfire_fabric`). Su comando es `/iceandfire` (find y fairy con trucos; los
  botones del altar usan `/iceandfire duel`), su config `config/iceandfire_fabric.json` y su jar
  `IceAndFire-Fabric-26.3-<versión>.jar`.
- Antes se llamaba iafabric. Un mundo de antes se abre igual: sus bloques, objetos, criaturas y estructuras se leen con
  el nombre nuevo, y lo que el mundo guarda (el hada y su progreso, los lazos, las ciudadelas liberadas, los lugares del
  hada) sigue guardado con el nombre viejo. La config vieja se mueve sola la primera vez. Solo se pierden los logros del
  mod, sus recetas desbloqueadas y las teclas que se hubieran cambiado.
