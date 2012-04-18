#include <blackadder.hpp>
#include <signal.h>

Blackadder *ba;
int payload_size = 1024;
int payload_count = 1024 * 500;
char *payload = (char *) malloc(payload_size);
bool stop = false;

void sigfun(int sig) {
	(void) signal(SIGINT, SIG_DFL);
	stop = true;
	ba->disconnect();
	free(payload);
	delete ba;
	exit(0);
}

int main(int argc, char* argv[]) {
	(void) signal(SIGINT, sigfun);
	ba = Blackadder::Instance(true);	//userspace
	
	cout << "Process ID: " << getpid() << endl;
	
	// publish
	string id = "0000000000000000";
	string prefix_id;
	string bin_id = hex_to_chararray(id);
	string bin_prefix_id=hex_to_chararray(prefix_id);
	
	ba->publish_scope(bin_id, bin_prefix_id, DOMAIN_LOCAL, NULL, 0);
	
	prefix_id = "0000000000000000";
	id = "1111111111111111";
	bin_id = hex_to_chararray(id);
	bin_prefix_id = hex_to_chararray(prefix_id);
	
	ba->publish_info(bin_id, bin_prefix_id, DOMAIN_LOCAL, NULL, 0);
	
	while(!stop) {
		Event ev;
		ba->getEvent(ev);
		switch (ev.type) {
			case SCOPE_PUBLISHED:
				cout << "SCOPE_PUBLISHED: " << chararray_to_hex(ev.id) << endl;
                break;
            case SCOPE_UNPUBLISHED:
                cout << "SCOPE_UNPUBLISHED: " << chararray_to_hex(ev.id) << endl;
				break;
            case START_PUBLISH:
                cout << "START_PUBLISH: " << chararray_to_hex(ev.id) << endl;
                for (int i = 0; i < payload_count; i++) {
                    ba->publish_data(ev.id, NODE_LOCAL, NULL, 0, payload, payload_size);
                }
		stop=true;
                break;
            case STOP_PUBLISH:
                cout << "STOP_PUBLISH: " << chararray_to_hex(ev.id) << endl;
                break;
            case PUBLISHED_DATA:
                cout << "PUBLISHED_DATA: " << chararray_to_hex(ev.id) << endl;
				break;
		}
	}
	sleep(5);
	free(payload);
	ba->disconnect();
	delete ba;
	return 0;
}

