Aplicacion Monolito Modular:

QUiero generar una aplicacion modular que este desarrollado en Thymeleaf para dar flexibilidad a los desarrollos y que tenga las siguientes funcionalidades, ahora te las menciono brevemente y luego te las desarrollo: 1ºWeb empresarial donde se promociona el negocio 2ºAceso al registro y la aplicacion Multitennant (Ya existe es otro micro que tengo creado te lo referencio aqui) la aplicacion multitennant tendra el nombre de SimbioSpere, para poder usar su api hay q revisar lo que expone. 3ºUna pagina de registro de usuarios de tipo cliente, 4º una pagina para que un administrado de de alta a usuarios de tipo trabajador. 5º Tener una serie de paginas que sirvan para gestionar los usuarios, de tipo trabajador y cliente. 6º Tener una seccion en la que los clientes pueden mandar solicitudes de trabajo, abra 2 tipos una sobre la aplicacion multitennant para sugerir nuevos desarrollo de modulos en SimbioSpere y luego otro tipo de solicitud independite q puede ser de cualquier tipo ejemplo una landing page, web empresarial a medida, app web. 7º Tener un portal de los usuarios empleados donde se puedan ver las solicitudes desearrollo de los tipos de : SimbioSpere o otro desarrollo. 8º Tener una seccion de contacto (hay q ver si los mensajes llegan a un correo o tenemos nuestro propio sistema)


AQUI desarrollo un poco mas todas las ideas para dar mas contexto:

0º
La parte front tiene que estar preparada para tener un modo escritorio y modo dispositivo movil.Adicionalmente se tiene que tener habilitado un modo oscuro y claro para todas las paginas.
Se tiene que tener un control que permita el multi idioma inicialmente se tendrian disponible Español e ingles. 

1ºPanel informativo de los servicios de la web
Se tienen que mostrar una pagina informativa de los servicios que tiene la pagina web, con un diseño atractivo e intuitivo q invite al usuario a explorar las posiblidades que ofrece esta plataforma.
Estaria interesante tener una imagen estatica en la pagina principal con el logotipo empresarial y cuando el usuario en la version de escritorio pasa el puntero por encima se inicie un breve video.
En la version de movil el video arrancaria nada mas q la pagina cargara.

2ºSe tiene que ofrecer un sistema de registro de usuarios, va haber dos tipos grandes de usuarios, los clientes y los trabajadores de la empresa.
A los usuarios q son trabajadores los tiene q dar de alta un usuario del sistema administrador.
Los usuarios clientes se pueden registrar a traves de la web o un usuario admin puede crear su usuario.
Los usuarios clientes pueden registrarse en una aplicacion que ofrece servicios multi tennat , su nombre esta por definir, el registro a esta aplicacion se tendra que hacer a traves de una api q expone este micro, el usuario cliente y el usuario de la aplicacion tennant estaran vinculados.La aplicacion multi tennant de momento solo ofrecera servicios para gestionar los pedidos de diferentes negocios especialente negocios de envio de comida a domicilio. Adicionamente negocios de reparacion. La idea en un futuro es ofrecer al cliente un selector de modulos y que configure su propio negocio.

3ºA la hora de ofrecer los servicios mencionados se tiene que dar la opcion de que el propio cliente solicite un desarrollo, ahora mismo sera de forma provisional gratuita pero en futuro se generara un cobro. La idea q en esta seccion el usuario solicite nuevos desarrollos para la Aplicacion de Multi tennant.
4º Adicionalmente si el usuario quiere algo especifico tambien se tiene que crear un boton con un formulario en el que introduce de formas sencilla una solicitud de desarrollo
5ºTodas estas solicitudes de desarrollo estaran visibles solamente para los usuarios empleados de la plataforma, tiene q haber una pagina en la que se visualicen todas esas solicitudes de desarrollo, estas solicitudes se podran filtrar por el tipo de desarrollo que se quiera , si esta vinculado a la aplicacion multitennant, si es un desarrollo independiente, si es una simple web, etc.
6º Los usuarios empleados tendran su propia web de gestion, donde visualizan como se ha mencionado previamente las solicitudes.
7ºLa solicitudes pueden asignarse a un o varios trabajadores, se registraran los estados de el desarrollo, las comunicaciones con el cliente y se registrara el presupuesto estimado.
8ºEn el registro de los usuarios empleados y clientes se solicitaran todos los datos basicos comunes de registro.
9º La idea central del negocio es que los empresarios tienen un negocio q requiere mucho tiempo atencion y gestion, la idea es ayudarles a simplificar todos los procesos de la empresa con el objetivo de aumentar el rendimiento del negocio con menos esfuerzo traduciendose en mas ingresos y mas tranquilidad. Tambien citar que en un futuro se implementaran agentes de IA que permitan automatizar tareas simples del negocio, habria q buscar algun tipo de slogan breve.
10ºMe gustaría que los estilos CSS se inspiraran en "C:\Users\x455035\1Git\AprendiendoIA\Symbio\symbio-frontend\src\app\globals.css", usando la paleta elegida y la pila tipográfica del sistema, sin fuentes web personalizadas.

11º Como mencionaba antes los usuarios de tipo cliente q se registran en esta aplicacion se tendran que vincular a los usuarios que son propietarios de los tennant, entonces hay q buscar la relacion en la base de datos para hacer el vinculo correcto. En el caso normal que un usuario cliente se registre en la web, y a su vez quiera contratar los servicios de la aplicacion multi tennant se registrar en las dos aplicaciones de golpe en vez primero en una y luegeo en otra. Si hay dudas al respecto no dudes en preguntar la ruta del proyecto MULTI TENNNANT ESTA AQUI: C:\Users\x455035\1Git\AprendiendoIA\ExampleProjects\DEMO-orders-thymeleaf


12ºHay que dejar una seccion del front que sea sobre nosotros o About us en el que se dejara un breve espacio para hablar sobre la empresa sus integrantes y la experiencia q se tiene

13ºLa amplicacion monolito tiene que tener un menu superior intuitivo tanto para la version de escritorio como para la version de movil, y preparado para el modo oscuro y claro, las opciones en modo escritorio se visualizaran si entran con el ancho de la pantalla si la anchura no es suficiente todas las opciones apareceran comprimidas en un boton q tiene 3 barras horizotales una debajo de otra, este boton estaria a la derecha puedes inspirarte en el MENU DE TENNANT AQUI TE LO REFERENCIO esta en este paquete pero no se exactamente donde: C:\Users\x455035\1Git\AprendiendoIA\ExampleProjects\DEMO-orders-thymeleaf\src\main\resources\templates

14º Adicionalmente se tiene que crear una seccion el la web que sea de contacto general con la empresa, este mensaje recibido no se considerara una solicitud de trabajo , si llegara como un mensaje al sistema interno de la app, creo que a lo mejor no es necesario mandarlo a un correo empresarial esta decision habria q revisarla por que no se que es mejor.
Lo suyo es que la comunicacion pueda ser agil, si el usuario desea mandar un mensaje de texto que tenga las opciones de mandar "un email(a cosiderar q opcion es mejor ) un whatsapp, una nota de audio, algun adjunto de fichero.
Si se opta por gestionar por la propia app los mensajes, los usuarios empleados deberian de tener una funcionabilidad q les permita visualizar la lista de mensajes, ver el hilo de mensajes , tener la opcion de archivar, responder borrar, vamos lo basico para una comunicacion.
La idea interesante de tener nosotros nuestro propio sistema es que podemos darle acceso a una ia ejemplo deepseek a nuestro sistema de correos y q responda a los clientes bajo unos criterios, es una idea de futuro.

15º El desarrollo de todo tipo de funcionabilidad, fix , modificacion cualquier cosa se tiene que hacer a traves de una skill que delimite como se tiene que programar para que el micro sea un monolito modular y se respete en todo momento

16ºLa aplicacion tendra en la raiz un readme.md en que servira como resumen de como esta estructurada la aplicacion como funciona, y tendra links a otras documentaciones que daran mas detalle.

17º Quiero que se genere documentacion de un inventario de todo lo que se tiene desarrollado

18º Quiero que en la raiz del proyecto haya un registro de todo lo desarrollado y su estado, seria como un diario


