# language: es
@acceptance
Característica: Gestión de tareas desde la interfaz web
  Como usuario del gestor de tareas
  Quiero registrar, completar y eliminar tareas desde el navegador
  Para llevar el control de mi trabajo pendiente

  # Estos escenarios son la barrera del Acceptance Test Gate:
  # se ejecutan contra la aplicación YA DESPLEGADA en el ambiente de prueba.
  # Si alguno falla, el pipeline detiene la promoción y ejecuta el rollback.

  Antecedentes:
    Dado que la aplicación está desplegada y disponible

  @smoke
  Escenario: La aplicación responde e informa qué versión está sirviendo
    Cuando abro el gestor de tareas
    Entonces la página muestra el identificador del slot desplegado
    Y el resumen indica 0 tareas totales

  Escenario: Registrar una tarea nueva
    Cuando abro el gestor de tareas
    Y registro la tarea "Preparar el informe" con prioridad "ALTA"
    Entonces la tarea "Preparar el informe" aparece en el listado
    Y el resumen indica 1 tareas totales
    Y el avance mostrado es 0 por ciento

  Escenario: Completar una tarea actualiza el avance
    Cuando abro el gestor de tareas
    Y registro la tarea "Revisar el pipeline" con prioridad "MEDIA"
    Y registro la tarea "Documentar el despliegue" con prioridad "BAJA"
    Y marco como completada la tarea "Revisar el pipeline"
    Entonces la tarea "Revisar el pipeline" figura como "Completada"
    Y el avance mostrado es 50 por ciento

  Escenario: Eliminar una tarea la quita del listado
    Cuando abro el gestor de tareas
    Y registro la tarea "Tarea descartable" con prioridad "BAJA"
    Y elimino la tarea "Tarea descartable"
    Entonces la tarea "Tarea descartable" ya no aparece en el listado

  Escenario: El sistema rechaza una tarea sin título
    Cuando abro el gestor de tareas
    Y registro la tarea "" con prioridad "MEDIA"
    Entonces se muestra el mensaje de error "obligatorio"
    Y el resumen indica 0 tareas totales

  Escenario: El sistema rechaza un título duplicado
    Cuando abro el gestor de tareas
    Y registro la tarea "Tarea única" con prioridad "ALTA"
    Y registro la tarea "TAREA ÚNICA" con prioridad "BAJA"
    Entonces se muestra el mensaje de error "Ya existe"
    Y el resumen indica 1 tareas totales

  Esquema del escenario: El avance se calcula según las tareas completadas
    Cuando abro el gestor de tareas
    Y registro <total> tareas de ejemplo
    Y completo las primeras <completadas> tareas
    Entonces el avance mostrado es <avance> por ciento

    Ejemplos:
      | total | completadas | avance |
      | 4     | 1           | 25     |
      | 4     | 2           | 50     |
      | 3     | 3           | 100    |
