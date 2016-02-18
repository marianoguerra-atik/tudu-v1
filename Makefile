.PHONY: setup

setup:
	mkdir -p tmp
	cd tmp && git clone https://github.com/omcljs/om.git && cd om && git checkout 0d31bd9baeb2bcbef96b4ebc4eef960fd46df1f0 && lein install
	rm -rf tmp/om

clean:
	lein clean
	rm -f *-init.clj figwheel_server.log
