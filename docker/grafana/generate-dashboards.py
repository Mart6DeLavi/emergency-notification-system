#!/usr/bin/env python3
"""Generate SENSA Grafana dashboards.

Run from the repository root:
    python3 docker/grafana/generate-dashboards.py
"""

import json
import os

BASE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(BASE, "dashboards")

PROM = {"type": "prometheus", "uid": "prometheus"}
LOKI = {"type": "loki", "uid": "loki"}

JAVA_SERVICES = [
    "authentication-service",
    "discovery-server",
    "filesystem-service",
    "notification-service",
    "template-service",
    "user-data-management-service",
]

RUST_SERVICES = [
    "emergency-situation-request-service",
    "message-delivery-service",
    "load-balancer-service",
    "rate-limiter-service",
    "api-gateway",
]

HIKARI_SERVICES = {"authentication-service", "filesystem-service"}


def target(expr, legend=None, ref="A", ds=None):
    item = {"refId": ref, "expr": expr, "datasource": ds or PROM}
    if legend:
        item["legendFormat"] = legend
    return item


def panel(
    title,
    kind,
    x,
    y,
    w,
    h,
    targets,
    unit=None,
    options=None,
    description="",
    decimals=None,
    thresholds=None,
    color_mode=None,
    fixed_color=None,
):
    item = {
        "title": title,
        "type": kind,
        "gridPos": {"x": x, "y": y, "w": w, "h": h},
        "datasource": PROM,
        "targets": targets,
        "fieldConfig": {"defaults": {}, "overrides": []},
        "options": options or {},
        "description": description,
    }
    if unit:
        item["fieldConfig"]["defaults"]["unit"] = unit
    if decimals is not None:
        item["fieldConfig"]["defaults"]["decimals"] = decimals
    if kind == "timeseries":
        item["options"].setdefault(
            "legend", {"displayMode": "list", "placement": "bottom", "showLegend": True}
        )
        item["options"].setdefault("tooltip", {"mode": "multi", "sort": "desc"})
    elif kind == "stat":
        item["options"].setdefault(
            "reduceOptions", {"calcs": ["lastNotNull"], "fields": "", "values": False}
        )
        item["options"]["colorMode"] = color_mode or "value"
        item["fieldConfig"]["defaults"]["thresholds"] = {
            "mode": "absolute",
            "steps": thresholds or [{"color": "green", "value": None}],
        }
        if fixed_color:
            item["fieldConfig"]["defaults"]["color"] = {"mode": "fixed", "fixedColor": fixed_color}
        item["options"].setdefault("graphMode", "area")
        item["options"].setdefault("justifyMode", "auto")
        item["options"].setdefault("orientation", "auto")
    elif kind == "logs":
        item["datasource"] = LOKI
        item["options"] = {
            "showTime": True,
            "sortOrder": "Descending",
            "wrapLogMessage": True,
            "enableLogDetails": True,
            "dedupStrategy": "none",
        }
    return item


def row(title, y):
    return {
        "title": title,
        "type": "row",
        "collapsed": False,
        "gridPos": {"x": 0, "y": y, "w": 24, "h": 1},
        "panels": [],
    }


def dashboard(uid, title, panels, refresh="30s", tags=None):
    return {
        "uid": uid,
        "title": title,
        "tags": tags or ["sensa"],
        "timezone": "browser",
        "schemaVersion": 39,
        "version": 1,
        "refresh": refresh,
        "editable": True,
        "graphTooltip": 1,
        "time": {"from": "now-6h", "to": "now"},
        "annotations": {"list": []},
        "links": [],
        "templating": {"list": []},
        "panels": panels,
    }


def write(rel_path, obj):
    full = os.path.join(OUT, rel_path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w", encoding="utf-8") as handle:
        json.dump(obj, handle, indent=2, ensure_ascii=False)
        handle.write("\n")
    print("wrote", os.path.relpath(full, BASE))


def build_server():
    p = []
    y = 0

    p.append(row("CPU", y))
    y += 1
    p.extend(
        [
            panel(
                "CPU usage",
                "stat",
                0,
                y,
                6,
                4,
                [target('100 - avg(irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100', "CPU %")],
                "percent",
                thresholds=[
                    {"color": "green", "value": None},
                    {"color": "yellow", "value": 70},
                    {"color": "red", "value": 85},
                ],
            ),
            panel(
                "Load average",
                "stat",
                6,
                y,
                6,
                4,
                [
                    target("node_load1", "1m"),
                    target("node_load5", "5m", "B"),
                    target("node_load15", "15m", "C"),
                ],
                "short",
            ),
            panel(
                "IO wait",
                "stat",
                12,
                y,
                6,
                4,
                [target('avg(irate(node_cpu_seconds_total{mode="iowait"}[5m])) * 100', "iowait %")],
                "percent",
                thresholds=[
                    {"color": "green", "value": None},
                    {"color": "yellow", "value": 5},
                    {"color": "red", "value": 15},
                ],
            ),
            panel(
                "CPU cores",
                "stat",
                18,
                y,
                6,
                4,
                [target("count(count by (cpu) (node_cpu_seconds_total))", "cores")],
                "short",
            ),
        ]
    )
    y += 4
    p.append(
        panel(
            "CPU by mode",
            "timeseries",
            0,
            y,
            12,
            8,
            [target("sum by (mode) (irate(node_cpu_seconds_total[5m]))", "{{mode}}")],
            "short",
        )
    )
    p.append(
        panel(
            "CPU per core",
            "timeseries",
            12,
            y,
            12,
            8,
            [
                target(
                    '100 - avg by (cpu) (irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100',
                    "core {{cpu}}",
                )
            ],
            "percent",
        )
    )
    y += 8

    p.append(row("Memory", y))
    y += 1
    p.extend(
        [
            panel(
                "Memory used",
                "stat",
                0,
                y,
                6,
                4,
                [target("(1 - node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes) * 100", "used %")],
                "percent",
                thresholds=[
                    {"color": "green", "value": None},
                    {"color": "yellow", "value": 70},
                    {"color": "red", "value": 85},
                ],
            ),
            panel(
                "Memory total",
                "stat",
                6,
                y,
                6,
                4,
                [target("node_memory_MemTotal_bytes", "total")],
                "bytes",
            ),
            panel(
                "Memory available",
                "stat",
                12,
                y,
                6,
                4,
                [target("node_memory_MemAvailable_bytes", "available")],
                "bytes",
            ),
            panel(
                "Swap used",
                "stat",
                18,
                y,
                6,
                4,
                [target("node_memory_SwapTotal_bytes - node_memory_SwapFree_bytes", "swap")],
                "bytes",
            ),
        ]
    )
    y += 4
    p.append(
        panel(
            "Memory breakdown",
            "timeseries",
            0,
            y,
            12,
            8,
            [
                target("node_memory_MemTotal_bytes", "total"),
                target("node_memory_MemAvailable_bytes", "available", "B"),
                target("node_memory_Cached_bytes", "cached", "C"),
                target("node_memory_Buffers_bytes", "buffers", "D"),
            ],
            "bytes",
        )
    )
    p.append(
        panel(
            "Swap",
            "timeseries",
            12,
            y,
            12,
            8,
            [target("node_memory_SwapTotal_bytes", "total"), target("node_memory_SwapFree_bytes", "free", "B")],
            "bytes",
        )
    )
    y += 8

    p.append(row("Disk", y))
    y += 1
    p.extend(
        [
            panel(
                "Disk total",
                "stat",
                0,
                y,
                8,
                4,
                [target('max(node_filesystem_size_bytes{mountpoint="/"})', "total")],
                "decbytes",
                decimals=1,
                color_mode="fixed",
                fixed_color="green",
                description="Total size of the root filesystem in gigabytes",
            ),
            panel(
                "Disk used",
                "stat",
                8,
                y,
                8,
                4,
                [
                    target(
                        'max(node_filesystem_size_bytes{mountpoint="/"}) - max(node_filesystem_free_bytes{mountpoint="/"})',
                        "used",
                    )
                ],
                "decbytes",
                decimals=1,
                color_mode="fixed",
                fixed_color="green",
                description="Space used on the root filesystem in gigabytes",
            ),
            panel(
                "Disk free",
                "stat",
                16,
                y,
                8,
                4,
                [target('max(node_filesystem_avail_bytes{mountpoint="/"})', "free")],
                "decbytes",
                decimals=1,
                color_mode="fixed",
                fixed_color="green",
                description="Space available on the root filesystem in gigabytes",
            ),
        ]
    )
    y += 4
    filesystem_filter = 'fstype!~"tmpfs|overlay|squashfs"'
    p.append(
        panel(
            "Disk usage by mount",
            "timeseries",
            0,
            y,
            12,
            8,
            [
                target(
                    f'100 - (node_filesystem_avail_bytes{{{filesystem_filter}}} / node_filesystem_size_bytes{{{filesystem_filter}}}) * 100',
                    "{{mountpoint}}",
                )
            ],
            "percent",
        )
    )
    p.append(
        panel(
            "Disk free by mount",
            "timeseries",
            12,
            y,
            12,
            8,
            [target(f"node_filesystem_avail_bytes{{{filesystem_filter}}}", "{{mountpoint}}")],
            "bytes",
        )
    )
    y += 8
    p.append(
        panel(
            "Inodes used by mount",
            "timeseries",
            0,
            y,
            12,
            8,
            [
                target(
                    f'100 - (node_filesystem_files_free{{{filesystem_filter}}} / node_filesystem_files{{{filesystem_filter}}}) * 100',
                    "{{mountpoint}}",
                )
            ],
            "percent",
        )
    )
    p.append(
        panel(
            "Disk throughput",
            "timeseries",
            12,
            y,
            12,
            8,
            [
                target("rate(node_disk_read_bytes_total[5m])", "read {{device}}"),
                target("rate(node_disk_written_bytes_total[5m])", "write {{device}}", "B"),
            ],
            "Bps",
        )
    )
    y += 8
    p.append(
        panel(
            "Disk IOPS",
            "timeseries",
            0,
            y,
            12,
            8,
            [
                target("rate(node_disk_reads_completed_total[5m])", "reads {{device}}"),
                target("rate(node_disk_writes_completed_total[5m])", "writes {{device}}", "B"),
            ],
            "short",
        )
    )
    p.append(
        panel(
            "Disk latency",
            "timeseries",
            12,
            y,
            12,
            8,
            [
                target(
                    "rate(node_disk_read_time_seconds_total[5m]) / rate(node_disk_reads_completed_total[5m])",
                    "read {{device}}",
                ),
                target(
                    "rate(node_disk_write_time_seconds_total[5m]) / rate(node_disk_writes_completed_total[5m])",
                    "write {{device}}",
                    "B",
                ),
            ],
            "s",
        )
    )
    y += 8

    p.append(row("Network", y))
    y += 1
    net_filter = 'device!~"lo|veth.*|docker.*|br-.*"'
    p.append(
        panel(
            "Network throughput",
            "timeseries",
            0,
            y,
            12,
            8,
            [
                target(f"rate(node_network_receive_bytes_total{{{net_filter}}}[5m])", "RX {{device}}"),
                target(f"rate(node_network_transmit_bytes_total{{{net_filter}}}[5m])", "TX {{device}}", "B"),
            ],
            "Bps",
        )
    )
    p.append(
        panel(
            "Network errors / drops",
            "timeseries",
            12,
            y,
            12,
            8,
            [
                target(f"rate(node_network_receive_errs_total{{{net_filter}}}[5m])", "RX errors {{device}}"),
                target(f"rate(node_network_receive_drop_total{{{net_filter}}}[5m])", "RX drops {{device}}", "B"),
                target(f"rate(node_network_transmit_errs_total{{{net_filter}}}[5m])", "TX errors {{device}}", "C"),
                target(f"rate(node_network_transmit_drop_total{{{net_filter}}}[5m])", "TX drops {{device}}", "D"),
            ],
            "short",
        )
    )
    y += 8

    p.append(row("System", y))
    y += 1
    p.extend(
        [
            panel(
                "Uptime",
                "stat",
                0,
                y,
                6,
                4,
                [target("node_time_seconds - node_boot_time_seconds", "uptime")],
                "s",
            ),
            panel(
                "Running processes",
                "stat",
                6,
                y,
                6,
                4,
                [target("node_procs_running", "running")],
                "short",
            ),
            panel(
                "Blocked processes",
                "stat",
                12,
                y,
                6,
                4,
                [target("node_procs_blocked", "blocked")],
                "short",
            ),
            panel(
                "Open file descriptors",
                "stat",
                18,
                y,
                6,
                4,
                [target("node_filefd_allocated", "allocated")],
                "short",
            ),
        ]
    )
    y += 4
    p.append(
        panel(
            "File descriptors",
            "timeseries",
            0,
            y,
            12,
            8,
            [target("node_filefd_allocated", "allocated"), target("node_filefd_maximum", "maximum", "B")],
            "short",
        )
    )
    p.append(
        panel(
            "Processes",
            "timeseries",
            12,
            y,
            12,
            8,
            [target("node_procs_running", "running"), target("node_procs_blocked", "blocked", "B")],
            "short",
        )
    )
    return dashboard("sensa-server", "SENSA · Server", p, tags=["sensa", "server"])


def build_services_overview():
    p = []
    y = 0

    p.append(row("Capacity", y))
    y += 1
    p.extend(
        [
            panel(
                "Active containers",
                "stat",
                0,
                y,
                6,
                4,
                [target('count(count by (name) (container_memory_working_set_bytes{name!=""}))', "containers")],
                "short",
            ),
            panel(
                "Services up",
                "stat",
                6,
                y,
                6,
                4,
                [target('sum(up{job=~"sensa-.*"})', "up")],
                "short",
            ),
            panel(
                "Services total",
                "stat",
                12,
                y,
                6,
                4,
                [target('count(up{job=~"sensa-.*"})', "total")],
                "short",
            ),
            panel(
                "Peak memory",
                "stat",
                18,
                y,
                6,
                4,
                [target('max(container_memory_working_set_bytes{name!=""})', "peak")],
                "bytes",
            ),
        ]
    )
    y += 4
    p.append(
        panel(
            "Peak CPU per container (top 3)",
            "timeseries",
            0,
            y,
            12,
            8,
            [
                target(
                    'topk(3, sum by (name) (rate(container_cpu_usage_seconds_total{name!=""}[5m])) * 100)',
                    "{{name}}",
                )
            ],
            "percent",
        )
    )
    p.append(
        panel(
            "Peak memory per container (top 5)",
            "timeseries",
            12,
            y,
            12,
            8,
            [target('topk(5, sum by (name) (container_memory_working_set_bytes{name!=""}))', "{{name}}")],
            "bytes",
        )
    )
    y += 8
    p.append(
        panel(
            "Ingress + Egress",
            "timeseries",
            0,
            y,
            12,
            8,
            [
                target('sum(rate(container_network_receive_bytes_total{name!=""}[5m]))', "ingress"),
                target('sum(rate(container_network_transmit_bytes_total{name!=""}[5m]))', "egress", "B"),
            ],
            "Bps",
        )
    )
    p.append(
        panel(
            "Container restarts (1h)",
            "timeseries",
            12,
            y,
            12,
            8,
            [target('changes(container_start_time_seconds{name!=""}[1h])', "{{name}}")],
            "short",
        )
    )
    y += 8
    p.append(
        panel(
            "CPU usage by container",
            "timeseries",
            0,
            y,
            12,
            10,
            [target('sum by (name) (rate(container_cpu_usage_seconds_total{name!=""}[5m])) * 100', "{{name}}")],
            "percent",
        )
    )
    p.append(
        panel(
            "Working set memory by container",
            "timeseries",
            12,
            y,
            12,
            10,
            [target('sum by (name) (container_memory_working_set_bytes{name!=""})', "{{name}}")],
            "bytes",
        )
    )
    y += 10
    p.append(
        panel(
            "Network RX by container",
            "timeseries",
            0,
            y,
            12,
            8,
            [target('sum by (name) (rate(container_network_receive_bytes_total{name!=""}[5m]))', "{{name}}")],
            "Bps",
        )
    )
    p.append(
        panel(
            "Network TX by container",
            "timeseries",
            12,
            y,
            12,
            8,
            [target('sum by (name) (rate(container_network_transmit_bytes_total{name!=""}[5m]))', "{{name}}")],
            "Bps",
        )
    )
    y += 8

    table = panel(
        "Service health (up)",
        "table",
        0,
        y,
        24,
        8,
        [target('up{job=~"sensa-.*"}', "{{instance}}")],
        "short",
    )
    table["targets"][0]["instant"] = True
    table["targets"][0]["format"] = "table"
    table["options"] = {"showHeader": True}
    p.append(table)
    y += 8

    p.append(row("Dependencies", y))
    y += 1
    p.extend(
        [
            panel("PostgreSQL up", "stat", 0, y, 6, 4, [target('pg_up{job="postgres-exporter"}', "pg")], "short"),
            panel("MongoDB up", "stat", 6, y, 6, 4, [target('mongodb_up{job="mongodb-exporter"}', "mongo")], "short"),
            panel("Redis up", "stat", 12, y, 6, 4, [target('redis_up{job="redis-exporter"}', "redis")], "short"),
            panel(
                "RabbitMQ queue",
                "stat",
                18,
                y,
                6,
                4,
                [target('rabbitmq_queue_messages', "messages")],
                "short",
            ),
        ]
    )
    y += 4
    p.append(
        panel(
            "Kafka consumer lag",
            "timeseries",
            0,
            y,
            12,
            8,
            [target("sum by (consumergroup) (kafka_consumergroup_lag)", "{{consumergroup}}")],
            "short",
        )
    )
    p.append(
        panel(
            "RabbitMQ queue depth",
            "timeseries",
            12,
            y,
            12,
            8,
            [target('rabbitmq_queue_messages', "notification.delivery")],
            "short",
        )
    )
    return dashboard("sensa-services-overview", "SENSA · Services overview", p, tags=["sensa", "services"])


def build_service(service, is_java):
    cd = f'container_label_com_docker_compose_service="{service}"'
    inst = f"{service}:.*"
    p = []
    y = 0

    p.append(row("Container", y))
    y += 1
    p.extend(
        [
            panel("Up", "stat", 0, y, 6, 4, [target(f'up{{instance=~"{inst}"}}', "up")], "short"),
            panel(
                "CPU",
                "stat",
                6,
                y,
                6,
                4,
                [target(f"sum(rate(container_cpu_usage_seconds_total{{{cd}}}[5m])) * 100", "CPU %")],
                "percent",
            ),
            panel(
                "Memory",
                "stat",
                12,
                y,
                6,
                4,
                [target(f"sum(container_memory_working_set_bytes{{{cd}}})", "working set")],
                "bytes",
            ),
            panel(
                "Restarts (1h)",
                "stat",
                18,
                y,
                6,
                4,
                [target(f"changes(container_start_time_seconds{{{cd}}}[1h])", "restarts")],
                "short",
            ),
        ]
    )
    y += 4
    p.append(
        panel(
            "CPU usage",
            "timeseries",
            0,
            y,
            12,
            8,
            [target(f"sum(rate(container_cpu_usage_seconds_total{{{cd}}}[5m])) * 100", "CPU %")],
            "percent",
        )
    )
    p.append(
        panel(
            "Memory usage",
            "timeseries",
            12,
            y,
            12,
            8,
            [target(f"sum(container_memory_working_set_bytes{{{cd}}})", "working set")],
            "bytes",
        )
    )
    y += 8
    p.append(
        panel(
            "Network RX",
            "timeseries",
            0,
            y,
            12,
            8,
            [target(f"sum(rate(container_network_receive_bytes_total{{{cd}}}[5m]))", "RX")],
            "Bps",
        )
    )
    p.append(
        panel(
            "Network TX",
            "timeseries",
            12,
            y,
            12,
            8,
            [target(f"sum(rate(container_network_transmit_bytes_total{{{cd}}}[5m]))", "TX")],
            "Bps",
        )
    )
    y += 8

    p.append(row("HTTP", y))
    y += 1
    if is_java:
        rps = f'sum(rate(http_server_requests_seconds_count{{instance=~"{inst}"}}[5m]))'
        err = (
            f'sum by (status) (rate(http_server_requests_seconds_count'
            f'{{instance=~"{inst}",status=~"4..|5.."}}[5m]))'
        )
        p95 = (
            f'histogram_quantile(0.95, sum by (le) (rate('
            f'http_server_requests_seconds_bucket{{instance=~"{inst}"}}[5m])))'
        )
    else:
        rps = f'sum(rate(axum_http_requests_total{{instance=~"{inst}"}}[5m]))'
        err = f'sum by (status) (rate(axum_http_requests_total{{instance=~"{inst}",status=~"4..|5.."}}[5m]))'
        p95 = (
            f'histogram_quantile(0.95, sum by (le) (rate('
            f'axum_http_requests_duration_seconds_bucket{{instance=~"{inst}"}}[5m])))'
        )
    p.append(panel("Requests per second", "timeseries", 0, y, 8, 8, [target(rps, "RPS")], "reqps"))
    p.append(panel("Errors 4xx / 5xx", "timeseries", 8, y, 8, 8, [target(err, "{{status}}")], "reqps"))
    p.append(panel("Latency p95", "timeseries", 16, y, 8, 8, [target(p95, "p95")], "s"))
    y += 8

    if is_java:
        p.append(row("JVM", y))
        y += 1
        p.append(
            panel(
                "Heap used",
                "timeseries",
                0,
                y,
                8,
                8,
                [target(f'sum(jvm_memory_used_bytes{{instance=~"{inst}",area="heap"}})', "heap")],
                "bytes",
            )
        )
        p.append(
            panel(
                "Threads",
                "timeseries",
                8,
                y,
                8,
                8,
                [target(f'sum(jvm_threads_live_threads{{instance=~"{inst}"}})', "threads")],
                "short",
            )
        )
        p.append(
            panel(
                "GC pause",
                "timeseries",
                16,
                y,
                8,
                8,
                [target(f'sum(rate(jvm_gc_pause_seconds_sum{{instance=~"{inst}"}}[5m]))', "pause")],
                "s",
            )
        )
        y += 8
        if service in HIKARI_SERVICES:
            p.append(
                panel(
                    "Hikari connections",
                    "timeseries",
                    0,
                    y,
                    24,
                    8,
                    [
                        target(f'sum(hikaricp_connections_active{{instance=~"{inst}"}})', "active"),
                        target(f'sum(hikaricp_connections_idle{{instance=~"{inst}"}})', "idle", "B"),
                        target(f'sum(hikaricp_connections_pending{{instance=~"{inst}"}})', "pending", "C"),
                    ],
                    "short",
                )
            )
            y += 8
    else:
        p.append(row("Runtime", y))
        y += 1
        p.append(
            panel(
                "Pending requests",
                "timeseries",
                0,
                y,
                12,
                8,
                [target(f'sum(axum_http_requests_pending{{instance=~"{inst}"}})', "pending")],
                "short",
            )
        )
        p.append(
            panel(
                "Process memory",
                "timeseries",
                12,
                y,
                12,
                8,
                [target(f"sum(container_memory_working_set_bytes{{{cd}}})", "working set")],
                "bytes",
            )
        )
        y += 8

    p.append(row("Logs", y))
    y += 1
    p.append(panel("Logs", "logs", 0, y, 24, 12, [target(f'{{service="{service}"}}', None, "A", LOKI)]))
    return dashboard(f"sensa-svc-{service}", f"SENSA · {service}", p, tags=["sensa", "service", service])


def main():
    write("Server/sensa-server.json", build_server())
    write("Services/sensa-services-overview.json", build_services_overview())
    for service in JAVA_SERVICES:
        write(f"Services/sensa-svc-{service}.json", build_service(service, True))
    for service in RUST_SERVICES:
        write(f"Services/sensa-svc-{service}.json", build_service(service, False))


if __name__ == "__main__":
    main()
