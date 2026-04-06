FROM loadimpact/k6:latest
WORKDIR /k6
COPY add_equipments_test.js .
ENTRYPOINT ["k6", "run", "add_equipments_test.js"]