FROM denvazh/gatling:2.3.1

COPY resource-usage-per-container.json /etc/grafana/dashboards/
COPY resource-usage-combined.json /etc/grafana/dashboards/

#ADD entrypoint.sh /entrypoint.sh
#RUN chmod +x -v /entrypoint.sh
ENTRYPOINT ["/stress-test.sh"]
