// One page, one section at a time; the hash keeps the open section in the address bar.
const sections = document.querySelectorAll('main > section');
const links = document.querySelectorAll('nav a');
let swaggerStarted = false;

function show() {
    const wanted = location.hash.slice(1);
    const open = document.getElementById(wanted) ? wanted : 'home';
    sections.forEach(section => section.hidden = section.id !== open);
    links.forEach(link => link.classList.toggle('active', link.getAttribute('href') === '#' + open));
    if (open === 'api' && !swaggerStarted) {
        // Started on first visit only, so the OpenAPI document isn't loaded for the other pages. Deep linking is off
        // because it would take over the hash.
        SwaggerUIBundle({
            url: '/q/openapi',
            dom_id: '#swagger-ui',
            deepLinking: false,
            presets: [SwaggerUIBundle.presets.apis]
        });
        swaggerStarted = true;
    }
    window.scrollTo(0, 0);
}

window.addEventListener('hashchange', show);
show();
