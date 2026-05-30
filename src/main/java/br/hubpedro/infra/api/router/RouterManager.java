package br.hubpedro.infra.api.router;

import br.hubpedro.contracts.HttpRequest;
import br.hubpedro.contracts.HttpResponse;
import br.hubpedro.contracts.RequestHandler;
import br.hubpedro.contracts.Router;
import br.hubpedro.contracts.annotations.Get;
import br.hubpedro.contracts.annotations.Post;

import java.lang.reflect.Method;

/**
 * Gerenciador de rotas que implementa a interface {@link Router}.
 * Permite tanto o registro manual de rotas quanto o registro automático
 * de controladores anotados com {@link Get} e {@link Post} usando reflexão.
 */
public class RouterManager implements Router {

    private final Router delegate;

    /**
     * Construtor padrão que inicializa com o DefaultRouter.
     */
    public RouterManager() {
        this(new DefaultRouter());
    }

    /**
     * Construtor que aceita uma implementação customizada do roteador.
     *
     * @param delegate o roteador que receberá o registro das rotas e fará a resolução
     */
    public RouterManager(Router delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("O roteador delegado não pode ser nulo.");
        }
        this.delegate = delegate;
    }

    @Override
    public void addRoute(String method, String url, RequestHandler handler) {
        delegate.addRoute(method, url, handler);
    }

    @Override
    public HttpResponse resolve(HttpRequest request) {
        return delegate.resolve(request);
    }

    /**
     * Varre todos os métodos declarados do controlador em busca das anotações
     * {@link Get} e {@link Post}, registrando as rotas encontradas no roteador.
     *
     * @param controller a instância do controlador a ser registrada
     */
    public void registerController(Object controller) {
        if (controller == null) {
            throw new IllegalArgumentException("O controlador não pode ser nulo.");
        }

        Class<?> clazz = controller.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Get.class)) {
                Get getAnn = method.getAnnotation(Get.class);
                String path = getAnn.value();
                addRoute("GET", path, new ReflectionMethodHandler(controller, method));
                System.out.println("Controlador registrado [GET]: " + path + " -> " + clazz.getSimpleName() + "." + method.getName());
            } else if (method.isAnnotationPresent(Post.class)) {
                Post postAnn = method.getAnnotation(Post.class);
                String path = postAnn.value();
                addRoute("POST", path, new ReflectionMethodHandler(controller, method));
                System.out.println("Controlador registrado [POST]: " + path + " -> " + clazz.getSimpleName() + "." + method.getName());
            }
        }
    }
}